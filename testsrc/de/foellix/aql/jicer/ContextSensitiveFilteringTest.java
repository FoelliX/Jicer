package de.foellix.aql.jicer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import de.foellix.aql.helper.Helper;
import de.foellix.aql.jicer.dg.DependenceGraph;
import soot.Unit;
import soot.jimple.Stmt;

@Tag("systemIsSetup")
public class ContextSensitiveFilteringTest {
	private static String debug;

	@Test
	public void test01() {
		test("-d " + debug
				+ " -f none -m slice -si -from \"Statement('getSimSerialNumber')->Method('source2')->App('example/IntentMatching/ImplicitIntentMatching1.apk')\" -to \"Statement('startActivity')->App('example/IntentMatching/ImplicitIntentMatching1.apk')\"",
				1);
	}

	@Test
	public void test02() {
		test("-d " + debug
				+ " -f none -ncsr -m slice -si -from \"Statement('getSimSerialNumber')->Method('source2')->App('example/IntentMatching/ImplicitIntentMatching1.apk')\" -to \"Statement('startActivity')->App('example/IntentMatching/ImplicitIntentMatching1.apk')\"",
				2);
	}

	private void test(String runCmd, int expected) {
		boolean noException = true;
		int count = 0;

		try {
			final String[] args = Helper.getRunCommandAsArray(runCmd);
			final DependenceGraph slicedSDG = new Jicer(args).jice();
			for (final Unit unit : slicedSDG.getAllNodes()) {
				if (unit instanceof Stmt) {
					final Stmt castedUnit = (Stmt) unit;
					if (castedUnit.containsInvokeExpr()) {
						if (castedUnit.getInvokeExpr().getMethod().getName().equals("outputReachabilityAndStart")) {
							count++;
						}
					}
				}
			}
		} catch (final Exception e) {
			noException = false;
			e.printStackTrace();
		}

		assertEquals(expected, count);
		assertTrue(noException);
	}
}