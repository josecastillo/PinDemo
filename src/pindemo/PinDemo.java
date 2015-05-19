package pindemo;

import javacard.framework.APDU;
import javacard.framework.Applet;
import javacard.framework.ISO7816;
import javacard.framework.ISOException;
import javacard.framework.OwnerPIN;
import javacard.framework.Util;

public class PinDemo extends Applet {
	
	private OwnerPIN pin;
	private static byte[] pinBytes = { 0x31, 0x32, 0x33, 0x34, 0x35, 0x36 };
	private static byte[] mockData = { 't', 'e', 's', 't', 'd', 'a', 't', 'a' };

	public static void install(byte[] bArray, short bOffset, byte bLength) {
		// GP-compliant JavaCard applet registration
		new PinDemo().register(bArray, (short) (bOffset + 1), bArray[bOffset]);
	}
	
	public PinDemo() {
		pin = new OwnerPIN((byte)3, (byte)6);
		pin.update(pinBytes, (short)0, (byte)pinBytes.length);
	}

	public void process(APDU apdu) {
		if (selectingApplet()) {
			return; // 0x9000
		}

		byte[] buf = apdu.getBuffer();
		if (buf[ISO7816.OFFSET_CLA] != ISO7816.CLA_ISO7816) {
			ISOException.throwIt(ISO7816.SW_CLA_NOT_SUPPORTED);
		}
		byte p1 = buf[ISO7816.OFFSET_P1];
		byte p2 = buf[ISO7816.OFFSET_P2];
		short p1p2 = Util.makeShort(p1, p2);
		switch (buf[ISO7816.OFFSET_INS]) {
		
			/** 
			 * INS:  0x20 VERIFY
			 * Verifies the PIN presented the data field.
			 * Ignores P1 and P2; there is only one PIN in the application.
			 */
			case (byte) 0x20: {
				short incomingLength = apdu.setIncomingAndReceive();
				// Check PIN
				if (!pin.check(apdu.getBuffer(), ISO7816.OFFSET_CDATA, (byte)incomingLength)) {
					ISOException.throwIt((short) (0x63C0 | pin.getTriesRemaining()));
				}
				break;
			}
			
			/** 
			 * INS:  0xCA GET DATA
			 * P1P2: 0x0100 (test data object)
			 * Returns the test data object if the PIN is verified.
			 * If the PIN is not verified, returns the status word 0x6982, "Security status not satisfied".
			 * If any other object is requested, returns the status word 0x6A83, "Record not found". 
			 */
			case (byte) 0xCA: {
				if (!pin.isValidated()) {
					ISOException.throwIt(ISO7816.SW_SECURITY_STATUS_NOT_SATISFIED);
				}
				if (p1p2 != 0x0100) {
					ISOException.throwIt(ISO7816.SW_RECORD_NOT_FOUND);
				}
				Util.arrayCopyNonAtomic(mockData, (short)0, apdu.getBuffer(), (short)0, (short)mockData.length);
				apdu.setOutgoingAndSend((short)0, (short)mockData.length);
				return;
			}
		default:
			ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
		}
	}

}
