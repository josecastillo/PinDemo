package pindemotest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertArrayEquals;

import javacard.framework.AID;
import pindemo.PinDemo;

import org.junit.Before;
import org.junit.Test;

import com.licel.jcardsim.base.Simulator;

public class PinDemoTest {
	Simulator simulator;
	static final byte[] pinDemoAid = new byte[] {(byte) 0xd2, 0x76, 0x00, (byte)0xFF, (byte)0xFE, 0x01, 0x02, 0x03, 0x04, 0x05};
	static final AID aid = new AID(pinDemoAid, (short)0, (byte)pinDemoAid.length);
	static final byte[] success = {(byte) 0x90, 0x00};
	static final byte[] securityNotSatisfied = {0x69, (byte)0x82};
	
	enum Direction {DIRECTION_INCOMING, DIRECTION_OUTGOING};
	
	@Before
	public void setup() {
		byte[] params = new byte[pinDemoAid.length + 1];
		params[0] = (byte) pinDemoAid.length;
		System.arraycopy(pinDemoAid, 0, params, 1, pinDemoAid.length);
		
		simulator = new Simulator();
		simulator.resetRuntime();
		simulator.installApplet(aid, PinDemo.class, params, (short)0, (byte) params.length);
		simulator.selectApplet(aid);
	}

	@Test
	public void testPin() {
		byte[] getDataCommand = {0, (byte) 0xca, 0x01, 0x00};
		byte[] getDataResponse = { 't', 'e', 's', 't', 'd', 'a', 't', 'a' , (byte) 0x90, 0x00};
		
		System.out.println("Test 1: GET DATA should fail before PIN is presented.");
		dumpHex(Direction.DIRECTION_OUTGOING, getDataCommand);
		byte[] resp = simulator.transmitCommand(getDataCommand);
		dumpHex(Direction.DIRECTION_INCOMING, resp);
		assertArrayEquals(securityNotSatisfied, resp);
		
		System.out.println("\nTest 2: After successful presentation of PIN, GET DATA should succeed.");
		assertEquals(true, doVerify("123456"));
		dumpHex(Direction.DIRECTION_OUTGOING, getDataCommand);
		resp = simulator.transmitCommand(getDataCommand);
		dumpHex(Direction.DIRECTION_INCOMING, resp);
		assertArrayEquals(getDataResponse, resp);
		
		simulator.reset();
		simulator.selectApplet(aid);
		
		System.out.println("\nTest 3: After a reset, but before PIN is presented, GET DATA command should fail.");
		dumpHex(Direction.DIRECTION_OUTGOING, getDataCommand);
		resp = simulator.transmitCommand(getDataCommand);
		dumpHex(Direction.DIRECTION_INCOMING, resp);
		assertArrayEquals(securityNotSatisfied, resp);
	}

	private boolean doVerify(String pin) {
		byte[] command = new byte[5 + pin.length()];
		command[1] = 0x20;
		command[4] = (byte) pin.length();
		int offs = 5;
		for(byte b : pin.getBytes()) {
			command[offs++] = b;
		}
		dumpHex(Direction.DIRECTION_OUTGOING, command);
		byte[] resp = simulator.transmitCommand(command);
		dumpHex(Direction.DIRECTION_INCOMING, resp);
		if(resp[0] == (byte)0x90 && resp[1] == 0x00) {
			return true;
		} else {
			return false;
		}
	}
	

	@SuppressWarnings("unused")
	private void dumpHex(Direction direction, byte[] data) {
		String out = (direction == Direction.DIRECTION_OUTGOING) ? " => " : " <= ";
		for(byte b : data) {
			out += String.format("%02x ", b);
		}
		System.out.println(out);
	}
}
