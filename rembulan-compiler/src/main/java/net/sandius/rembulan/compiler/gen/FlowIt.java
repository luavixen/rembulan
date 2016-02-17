package net.sandius.rembulan.compiler.gen;

import net.sandius.rembulan.compiler.gen.block.AccountingNode;
import net.sandius.rembulan.compiler.gen.block.Entry;
import net.sandius.rembulan.compiler.gen.block.LineInfo;
import net.sandius.rembulan.compiler.gen.block.Linear;
import net.sandius.rembulan.compiler.gen.block.LinearSeq;
import net.sandius.rembulan.compiler.gen.block.LinearSeqTransformation;
import net.sandius.rembulan.compiler.gen.block.Node;
import net.sandius.rembulan.compiler.gen.block.Nodes;
import net.sandius.rembulan.compiler.gen.block.Target;
import net.sandius.rembulan.lbc.Prototype;
import net.sandius.rembulan.util.Graph;
import net.sandius.rembulan.util.IntVector;
import net.sandius.rembulan.util.ReadOnlyArray;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class FlowIt {

	private final Prototype prototype;

	private final Map<Prototype, Unit> units;

	public FlowIt(Prototype prototype) {
		this.prototype = Objects.requireNonNull(prototype);
		this.units = new HashMap<>();
	}

	@Deprecated
	public void go() {
		addUnits(prototype);

		for (Unit u : units.values()) {
			processGeneric(u);
		}
	}

	private void addUnits(Prototype prototype) {
		if (!units.containsKey(prototype)) {
			Unit u = initUnit(prototype);
			units.put(prototype, u);
			for (Prototype np : prototype.getNestedPrototypes()) {
				addUnits(np);
			}
		}
	}

	private Unit initUnit(Prototype prototype) {
		Unit unit = new Unit(prototype);
		unit.initGeneric();
		return unit;
	}

	private void processGeneric(Unit unit) {
		CompiledPrototype cp = unit.generic();

		cp.insertHooks();

		cp.inlineInnerJumps();
		cp.makeBlocks();

		Nodes.applyTransformation(cp.callEntry, new CollectCPUAccounting());

		// remove repeated line info nodes
		Nodes.applyTransformation(cp.callEntry, new RemoveRedundantLineNodes());

		// dissolve blocks
		cp.dissolveBlocks();

		// remove all line info nodes
//		applyTransformation(entryPoints, new LinearSeqTransformation.Remove(Predicates.isClass(LineInfo.class)));

//		System.out.println();
//		printNodes(entryPoints);

		cp.updateDataFlow();

		cp.inlineBranches();

		// add capture nodes
		cp.insertCaptureNodes();

//		addResumptionPoints();

		cp.makeBlocks();

		cp.updateDataFlow();

		cp.computeReturnType();
	}

	protected Unit mainUnit() {
		return units.get(prototype);
	}

	@Deprecated
	public Type.FunctionType functionType() {
		return mainUnit().generic().functionType();
	}

	@Deprecated
	public Graph<Node> nodeGraph() {
		return mainUnit().generic().nodeGraph();
	}

	@Deprecated
	public Map<Prototype, Set<TypeSeq>> callSites() {
		return mainUnit().generic().callSites();
	}

	private static class CollectCPUAccounting extends LinearSeqTransformation {

		@Override
		public void apply(LinearSeq seq) {
			List<AccountingNode> toBeRemoved = new ArrayList<>();

			int cost = 0;

			for (Linear n : seq.nodes()) {
				if (n instanceof AccountingNode) {
					AccountingNode an = (AccountingNode) n;
					if (n instanceof AccountingNode.TickBefore) {
						cost += 1;
						toBeRemoved.add(an);
					}
					else if (n instanceof AccountingNode.Add) {
						cost += ((AccountingNode.Add) n).cost;
						toBeRemoved.add(an);
					}
				}
			}

			for (AccountingNode an : toBeRemoved) {
				// remove all nodes
				an.remove();
			}

			if (cost > 0) {
				// insert cost node at the beginning
				seq.insertAtBeginning(new AccountingNode.Add(cost));
			}
		}

	}

	private static class RemoveRedundantLineNodes extends LinearSeqTransformation {

		@Override
		public void apply(LinearSeq seq) {
			int line = -1;
			List<Linear> toBeRemoved = new ArrayList<>();

			for (Linear n : seq.nodes()) {
				if (n instanceof LineInfo) {
					LineInfo lineInfoNode = (LineInfo) n;
					if (lineInfoNode.line == line) {
						// no need to keep this one
						toBeRemoved.add(lineInfoNode);
					}
					line = lineInfoNode.line;
				}
			}

			for (Linear n : toBeRemoved) {
				n.remove();
			}

		}

	}

}
