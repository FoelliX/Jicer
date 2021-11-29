package de.foellix.aql.jicer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import de.foellix.aql.helper.Helper;
import de.foellix.aql.jicer.statistics.Statistics;

@Tag("longLasting")
@Tag("systemIsSetup")
public class JicerResetTest {
	@Test
	public void test01() {
		// DroidBench (29 - normal)
		test(39, 5,
				"-d debug -f none -m slice -from \"Statement('$r4 = virtualinvoke $r3.<android.telephony.TelephonyManager: java.lang.String getDeviceId()>()')->Method('<de.ecspride.Button2: void clickOnButton3(android.view.View)>')->Class('de.ecspride.Button2')->App('example/DroidBench/Callbacks/Button2.apk')\" -to \"Statement('virtualinvoke $r2.<android.telephony.SmsManager: void sendTextMessage(java.lang.String,java.lang.String,java.lang.String,android.app.PendingIntent,android.app.PendingIntent)>(#QUOTE#+49 1234#QUOTE#, null, $r4, null, null)')->Method('<de.ecspride.Button2$1: void onClick(android.view.View)>')->Class('de.ecspride.Button2$1')->App('example/DroidBench/Callbacks/Button2.apk')\"");
	}

	@Test
	public void test02() {
		// DroidBench (29 - runnable)
		test(36, 5,
				"-d debug -f apk -m slice -run -from \"Statement('$r4 = virtualinvoke $r3.<android.telephony.TelephonyManager: java.lang.String getDeviceId()>()')->Method('<de.ecspride.Button2: void clickOnButton3(android.view.View)>')->Class('de.ecspride.Button2')->App('example/DroidBench/Callbacks/Button2.apk')\" -to \"Statement('virtualinvoke $r2.<android.telephony.SmsManager: void sendTextMessage(java.lang.String,java.lang.String,java.lang.String,android.app.PendingIntent,android.app.PendingIntent)>(#QUOTE#+49 1234#QUOTE#, null, $r4, null, null)')->Method('<de.ecspride.Button2$1: void onClick(android.view.View)>')->Class('de.ecspride.Button2$1')->App('example/DroidBench/Callbacks/Button2.apk')\"");
	}

	@Test
	public void test03() {
		// TaintBench (15 - runnable)
		test(6904, 5,
				"-d debug -f none -m slice -run -from \"Statement('$r9 = virtualinvoke $r3.<android.content.ContentResolver: android.database.Cursor query(android.net.Uri,java.lang.String[],java.lang.String,java.lang.String[],java.lang.String)>($r4, null, $r8, null, null)', 194)->Method('<com.beita.contact.MyContacts: java.util.List getContactsInfoListFromPhone()>')->Class('com.beita.contact.MyContacts')->App('example/TaintBench/beita_com_beita_contact.apk')\" -to \"Statement('virtualinvoke $r2.<java.io.BufferedWriter: void write(java.lang.String)>($r0)', 12)->Method('<com.beita.contact.ContactUtil: void write(java.lang.String,java.lang.String)>')->Class('com.beita.contact.ContactUtil')->App('example/TaintBench/beita_com_beita_contact.apk')\"");
	}

	// @Test
	public void test04() {
		// TaintBench (16 - normal)
		// 18768 <- in combination with tests before
		// 18808 <- 2x in combination with tests before
		// 3 times when run solo: 18794
		// TODO: Handle non-determinism when not restarting. (1/3)
		test(18794, 3,
				"-d debug -f none -m slice -from \"Statement('$r4 = interfaceinvoke $r1.<android.database.Cursor: java.lang.String getString(int)>($i0)', 373)->Method('<ca.ji.no.method10.BaiduUtils$2: void run()>')->Class('ca.ji.no.method10.BaiduUtils$2')->App('example/TaintBench/cajino_baidu.apk')\" -to \"Statement('virtualinvoke $r3.<java.io.FileWriter: void write(java.lang.String)>($r1)', 479)->Method('<ca.ji.no.method10.BaiduUtils: void createContactFile(java.lang.String,java.lang.String)>')->Class('ca.ji.no.method10.BaiduUtils')->App('example/TaintBench/cajino_baidu.apk')\"");
	}

	// @Test
	public void test05() {
		// TaintBench (124 - runnable)
		// TODO: Handle non-determinism when not restarting. (2/3)
		test(45420, 2,
				"-d debug -f none -m slice -run -from \"Statement('$r13 = interfaceinvoke $r10.<android.database.Cursor: java.lang.String getString(int)>(1)', 1182)->Method('<com.android.service.XmppService: void Calendar_saveFile()>')->Class('com.android.service.XmppService')->App('example/TaintBench/remote_control_smack.apk')\" -to \"Statement('virtualinvoke $r2.<java.io.FileWriter: java.io.Writer append(java.lang.CharSequence)>($r6)', 1208)->Method('<com.android.service.XmppService: void Calendar_saveFile()>')->Class('com.android.service.XmppService')->App('example/TaintBench/remote_control_smack.apk')\"");
	}

	// @Test
	public void test06() {
		// TaintBench (124 - normal)
		// 45153
		// TODO: Handle non-determinism when not restarting. (3/3)
		test(45262, 2,
				"-d debug -f none -m slice -from \"Statement('$r13 = interfaceinvoke $r10.<android.database.Cursor: java.lang.String getString(int)>(1)', 1182)->Method('<com.android.service.XmppService: void Calendar_saveFile()>')->Class('com.android.service.XmppService')->App('example/TaintBench/remote_control_smack.apk')\" -to \"Statement('virtualinvoke $r2.<java.io.FileWriter: java.io.Writer append(java.lang.CharSequence)>($r6)', 1208)->Method('<com.android.service.XmppService: void Calendar_saveFile()>')->Class('com.android.service.XmppService')->App('example/TaintBench/remote_control_smack.apk')\"");
	}

	private void test(int statementsToBeSliced, int attempts, String runCmd) {
		boolean noException = true;

		try {
			final String[] args = Helper.getRunCommandAsArray(runCmd);
			final Jicer jicer = new Jicer(args);
			for (int i = 0; i < attempts; i++) {
				jicer.jice();
				assertEquals(statementsToBeSliced,
						Statistics.getCounter(Statistics.COUNTER_STATEMENTS_SLICED).getCounter());
				jicer.reset();
			}
		} catch (final Exception e) {
			noException = false;
			e.printStackTrace();
		}

		assertTrue(noException);
	}
}