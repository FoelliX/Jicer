package de.foellix.aql.jicer.soot;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import de.foellix.aql.Log;
import de.foellix.aql.jicer.Data;
import de.foellix.aql.jicer.Parameters;
import de.foellix.aql.jicer.dg.DependenceGraph;
import de.foellix.aql.jicer.dg.PDGHelper;
import de.foellix.aql.jicer.statistics.Statistics;
import soot.Body;
import soot.BodyTransformer;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.jimple.infoflow.collect.ConcurrentHashSet;

public class PDGTransformer extends BodyTransformer {
	private int max = 0;
	private int counter = 0;
	private int done = 0;

	private Set<String> running = new ConcurrentHashSet<>();

	private Lock lock = new ReentrantLock();

	public PDGTransformer() {
		super();
		for (final SootClass sc : Scene.v().getApplicationClasses()) {
			if (sc.isConcrete()) {
				for (final SootMethod sm : sc.getMethods()) {
					if (sm.isConcrete() && (Parameters.getInstance().isSliceOrdinaryLibraryPackages()
							|| !SootHelper.isOrdinaryLibraryMethod(sm, false))) {
						this.max++;
					}
				}
			}
		}
		Statistics.getCounter(Statistics.COUNTER_PDGS).increase(this.max);
	}

	@Override
	protected void internalTransform(Body b, String phaseName, Map<String, String> options) {
		if (!b.getMethod().getDeclaringClass().isConcrete() || !b.getMethod().isConcrete()
				|| (!Parameters.getInstance().isSliceOrdinaryLibraryPackages()
						&& SootHelper.isOrdinaryLibraryMethod(b.getMethod(), false))) {
			return;
		}

		this.lock.lock();
		if (this.counter == 0 && Log.logIt(Log.DEBUG)) {
			new Thread(() -> {
				while (this.done < this.max) {
					try {
						Thread.sleep(30000);
					} catch (final InterruptedException e) {
						// do nothing
					}
					if (this.done < this.max) {
						if (!this.running.isEmpty()) {
							Log.msg("Still computing the following PDG(s): " + this.running, Log.DEBUG);
						}
					}
				}
			}).start();
		}
		this.counter++;
		this.lock.unlock();
		final String local = this.counter + "/" + this.max + ": Building PDG for " + b.getMethod();
		this.running.add(local);
		Log.msg(local, Log.NORMAL, true, true);

		// Get PDG
		final DependenceGraph pdg = PDGHelper.getPDG(b);

		// Add to collection
		Data.getInstance().addPdg(b.getMethod(), pdg);

		// Check if done
		this.lock.lock();
		this.done++;
		this.lock.unlock();
		this.running.remove(local);
	}
}