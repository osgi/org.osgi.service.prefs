/*******************************************************************************
 * Copyright (c) Contributors to the Eclipse Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0 
 *******************************************************************************/
/**
 * OSGi Test Suite Implementation. OSGi Confidential.
 */

package org.osgi.test.cases.prefs.junit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.api.Assertions.within;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;
import org.osgi.service.prefs.PreferencesService;

/**
 * This is the bundle initially installed and started by the TestCase when
 * started. It performs the various tests and reports back to the TestCase.
 */
public class PrefsControl extends PrefsTestSupport {
	private PreferencesService	prefs;

	@BeforeEach
	public void setUp() {
		prefs = getService(PreferencesService.class);
		assertThat(prefs).isNotNull();
	}

	@AfterEach
	public void tearDown() {
		ungetService(prefs);
	}

	@Test
	public void testSystemRoot() {
		Preferences theNode = prefs.getSystemPreferences();
		runAllTests(theNode, "/");
	}

	@Test
	public void testSystemNode() {
		Preferences theNode = prefs.getSystemPreferences().node(
				"somenode/anothernode");
		runAllTests(theNode, "/somenode/anothernode");
	}

	@Test
	public void testUserRoot() {
		Preferences theNode = prefs.getUserPreferences("theuser");
		runAllTests(theNode, "/");
	}

	@Test
	public void testUserNode() {
		Preferences theNode = prefs.getUserPreferences("theuser").node(
				"somenode/anothernode");
		runAllTests(theNode, "/somenode/anothernode");
	}

	@Test
	public void testRemovedNode() {
		Preferences theNode = prefs.getUserPreferences("theuser").node(
				"removednode");
		testRemoveNode(theNode);
		runRemovedTests(theNode, "/removednode");
	}

	@Test
	public void testRemovedAncestor() {
		Preferences theNode = prefs.getUserPreferences("theuser").node(
				"removedancestor/child/baby");
		Preferences ancestor = prefs.getUserPreferences("theuser").node(
				"removedancestor");
		testRemoveNode(ancestor);
		runRemovedTests(theNode, "/removedancestor/child/baby");
	}

	private void runAllTests(Preferences theNode, String absPath) {
		/* Test flush and sync */
		flush(theNode);
		sync(theNode);

		/* Test paths, names and if node exists */
		testAbsolutePath(theNode, absPath);
		testParent(theNode, absPath);
		testNodeExists(theNode, "");
		testName(theNode, absPath);
		testUnusualNames(theNode, absPath);
		testIllegalNames(theNode);

		/* Test if properties and node names are case sensitive */
		testCaseSensitivity(theNode);

		/* Test get and put methods */
		testGetMethods(theNode);
		testPutMethods(theNode);

		/* Test reading and writing properties from different types */
		testPropValues(theNode);

		/* Test checking for keys, removing and clearing properties */
		testKeys(theNode);

		/* Test adding children */
		testChildren(theNode);

		/* Test removing the node */
		testRemoveNode(theNode);
	}

	/*
	 * This method runs useful tests when a node is supposed to have been
	 * removed
	 */
	private void runRemovedTests(Preferences theNode, String absPath) {
		try {
			/* Test flush and sync */
			try {
				theNode.flush();
				fail("removed");
			}
			catch (IllegalStateException e) {
				// expected
			}
			try {
				theNode.sync();
				fail("removed");
			}
			catch (IllegalStateException e) {
				// expected
			}

			/* Test paths, names and if node exists */
			testAbsolutePath(theNode, absPath);
			assertThat(theNode.nodeExists("")).isFalse();
			testName(theNode, absPath);

			/* Test get and put methods */
			testGetMethodsRemoved(theNode);
			testPutMethodsRemoved(theNode);

			/* Test checking for keys, removing and clearing properties */
			try {
				theNode.put("somekey", "somevalue");
				fail("removed");
			}
			catch (IllegalStateException e) {
				// expected
			}
			try {
				theNode.remove("somekey");
				fail("removed");
			}
			catch (IllegalStateException e) {
				// expected
			}
			try {
				theNode.clear();
				fail("removed");
			}
			catch (IllegalStateException e) {
				// expected
			}

			/* Test adding children */
			try {
				theNode.node("child1");
				fail("removed");
			}
			catch (IllegalStateException e) {
				// expected
			}
			try {
				theNode.nodeExists("child1");
				fail("removed");
			}
			catch (IllegalStateException e) {
				// expected
			}

			/* Test removing the node */
			try {
				theNode.removeNode();
				fail("removed");
			}
			catch (IllegalStateException e) {
				// expected
			}
		}
		catch (Exception e) {
			fail("exception", e);
		}
	}

	/* The test methods */
	/* These methods sets the requirement markers and performs tests */

	private void testAbsolutePath(Preferences theNode, String absPath) {
		assertThat(theNode.absolutePath()).isEqualTo(absPath);
	}

	private void testChildren(Preferences theNode) {
		clearChildren(theNode);

		createNode(theNode, "child1");
		createNode(theNode, "child2");
		createNode(theNode, "child3");
		testNodeExists(theNode, "child1");
		testNodeExists(theNode, "child2");
		testNodeExists(theNode, "child3");
		try {
			assertThat(theNode.childrenNames()).hasSize(3);
		}
		catch (Exception e) {
			fail("children", e);
		}
		clearChildren(theNode);
	}

	private void testGetMethods(Preferences theNode) {
		clearKeys(theNode);
		assertThat(theNode.get("stringkey", "(empty)")).isEqualTo("(empty)");
		assertThat(theNode.getBoolean("booleankey", false)).isFalse();
		assertThat(theNode.getByteArray("bytearraykey", new byte[]{})).isEmpty();
		assertThat(theNode.getDouble("doublekey", 0.0d)).isCloseTo(0.0d, within(0.0d));
		assertThat(theNode.getFloat("floatkey", 0.0f)).isCloseTo(0.0f, within(0.0f));
		assertThat(theNode.getInt("integerkey", 0)).isZero();
		assertThat(theNode.getLong("longkey", 0l)).isZero();

		assertThat(theNode.get("stringkey", null)).isNull();
		assertThat(theNode.getByteArray("bytearraykey", null)).isNull();

		try {
			try {
				theNode.get(null, "(empty)");
				fail("expected NullPointerException");
			}
			catch (NullPointerException e) {
				// expected
			}
			try {
				theNode.getBoolean(null, false);
				fail("expected NullPointerException");
			}
			catch (NullPointerException e) {
				// expected
			}
			try {
				theNode.getByteArray(null, new byte[] {});
				fail("expected NullPointerException");
			}
			catch (NullPointerException e) {
				// expected
			}
			try {
				theNode.getDouble(null, 0.0d);
				fail("expected NullPointerException");
			}
			catch (NullPointerException e) {
				// expected
			}
			try {
				theNode.getFloat(null, 0.0f);
				fail("expected NullPointerException");
			}
			catch (NullPointerException e) {
				// expected
			}
			try {
				theNode.getInt(null, 0);
				fail("expected NullPointerException");
			}
			catch (NullPointerException e) {
				// expected
			}
			try {
				theNode.getLong(null, 0l);
				fail("expected NullPointerException");
			}
			catch (NullPointerException e) {
				// expected
			}
		}
		catch (Exception e) {
			fail("unexpected", e);
		}
	}

	private void testPutMethods(Preferences theNode) {
		clearKeys(theNode);
		theNode.put("stringkey", "(empty)");
		theNode.putBoolean("booleankey", false);
		theNode.putByteArray("bytearraykey", new byte[] {});
		theNode.putDouble("doublekey", 0.0d);
		theNode.putFloat("floatkey", 0.0f);
		theNode.putInt("integerkey", 0);
		theNode.putLong("longkey", 0l);

		assertThat(theNode.get("stringkey", null)).isEqualTo("(empty)");
		assertThat(theNode.getBoolean("booleankey", true)).isFalse();
		assertThat(theNode.getByteArray("bytearraykey", null)).isEmpty();
		assertThat(theNode.getDouble("doublekey", 1.0d)).isCloseTo(0.0d, within(0.0d));
		assertThat(theNode.getFloat("floatkey", 1.0f)).isCloseTo(0.0f, within(0.0f));
		assertThat(theNode.getInt("integerkey", 1)).isZero();
		assertThat(theNode.getLong("longkey", 1l)).isZero();

		try {
			try {
				theNode.put("stringkey", null);
				fail("expected NullPointerException");
			}
			catch (NullPointerException e) {
				// expected
			}
			try {
				theNode.putByteArray("bytearraykey", null);
				fail("expected NullPointerException");
			}
			catch (NullPointerException e) {
				// expected
			}
			try {
				theNode.put(null, "(empty)");
				fail("expected NullPointerException");
			}
			catch (NullPointerException e) {
				// expected
			}
			try {
				theNode.putBoolean(null, false);
				fail("expected NullPointerException");
			}
			catch (NullPointerException e) {
				// expected
			}
			try {
				theNode.putByteArray(null, new byte[] {});
				fail("expected NullPointerException");
			}
			catch (NullPointerException e) {
				// expected
			}
			try {
				theNode.putDouble(null, 0.0d);
				fail("expected NullPointerException");
			}
			catch (NullPointerException e) {
				// expected
			}
			try {
				theNode.putFloat(null, 0.0f);
				fail("expected NullPointerException");
			}
			catch (NullPointerException e) {
				// expected
			}
			try {
				theNode.putInt(null, 0);
				fail("expected NullPointerException");
			}
			catch (NullPointerException e) {
				// expected
			}
			try {
				theNode.putLong(null, 0l);
				fail("expected NullPointerException");
			}
			catch (NullPointerException e) {
				// expected
			}
		}
		catch (Exception e) {
			fail("unexpected", e);
		}
	}

	private void testGetMethodsRemoved(Preferences theNode) {
		try {
			theNode.get("stringkey", "(empty)");
			fail("missing IllegalStateException");
		}
		catch (IllegalStateException e) {
			// expected
		}
		try {
			theNode.getBoolean("booleankey", false);
			fail("missing IllegalStateException");
		}
		catch (IllegalStateException e) {
			// expected
		}
		try {
			theNode.getByteArray("bytearraykey", new byte[] {});
			fail("missing IllegalStateException");
		}
		catch (IllegalStateException e) {
			// expected
		}
		try {
			theNode.getDouble("doublekey", 0.0d);
			fail("missing IllegalStateException");
		}
		catch (IllegalStateException e) {
			// expected
		}
		try {
			theNode.getFloat("floatkey", 0.0f);
			fail("missing IllegalStateException");
		}
		catch (IllegalStateException e) {
			// expected
		}
		try {
			theNode.getInt("integerkey", 0);
			fail("missing IllegalStateException");
		}
		catch (IllegalStateException e) {
			// expected
		}
		try {
			theNode.getLong("longkey", 0l);
			fail("missing IllegalStateException");
		}
		catch (IllegalStateException e) {
			// expected
		}
	}

	private void testPutMethodsRemoved(Preferences theNode) {
		try {
			theNode.put("stringkey", "(empty)");
			fail("missing IllegalStateException");
		}
		catch (IllegalStateException e) {
			// expected
		}
		try {
			theNode.putBoolean("booleankey", false);
			fail("missing IllegalStateException");
		}
		catch (IllegalStateException e) {
			// expected
		}
		try {
			theNode.putByteArray("bytearraykey", new byte[] {});
			fail("missing IllegalStateException");
		}
		catch (IllegalStateException e) {
			// expected
		}
		try {
			theNode.putDouble("doublekey", 0.0d);
			fail("missing IllegalStateException");
		}
		catch (IllegalStateException e) {
			// expected
		}
		try {
			theNode.putFloat("floatkey", 0.0f);
			fail("missing IllegalStateException");
		}
		catch (IllegalStateException e) {
			// expected
		}
		try {
			theNode.putInt("integerkey", 0);
			fail("missing IllegalStateException");
		}
		catch (IllegalStateException e) {
			// expected
		}
		try {
			theNode.putLong("longkey", 0l);
			fail("missing IllegalStateException");
		}
		catch (IllegalStateException e) {
			// expected
		}
	}

	private void testKeys(Preferences theNode) {
		try {
			clearKeys(theNode);

			theNode.put("somekey", "somevalue");
			theNode.put("someotherkey", "someothervalue");

			List<String> keys = Arrays.asList(theNode.keys());
			assertThat(keys.size()).isEqualTo(2);
			assertThat(keys.contains("somekey")).isTrue();
			assertThat(keys.contains("someotherkey")).isTrue();

			testRemove(theNode, "somekey");

			keys = Arrays.asList(theNode.keys());
			assertThat(keys.size()).isEqualTo(1);
			assertThat(keys.contains("somekey")).isFalse();
			assertThat(keys.contains("someotherkey")).isTrue();

			clearKeys(theNode);

			keys = Arrays.asList(theNode.keys());
			assertThat(keys.size()).isEqualTo(0);
			assertThat(keys.contains("somekey")).isFalse();
			assertThat(keys.contains("someotherkey")).isFalse();
		}
		catch (Exception e) {
			fail("keys error", e);
		}
	}

	private void testName(Preferences theNode, String absPath) {
		assertThat(theNode.name()).isEqualTo(getNodeName(absPath));
	}

	private static String getNodeName(String absPath) {
		int i = absPath.lastIndexOf("/");
		return absPath.substring(i + 1);
	}

	private static String getAbsPathName(String absPath, String nodeName) {
		if (nodeName.startsWith("/")) {
			return nodeName;
		}
		if (absPath.equals("/")) {
			return "/" + nodeName;
		}
		return absPath + "/" + nodeName;
	}

	private void createNode(Preferences theNode, String pathName) {
		try {
			Preferences theNewNode = theNode.node(pathName);
			assertThat(theNewNode.absolutePath()).isEqualTo(getAbsPathName(theNode.absolutePath(), pathName));
		}
		catch (Exception e) {
			fail("create node", e);
		}
	}

	private void testNodeExists(Preferences theNode, String pathName) {
		/* Test the relative path */
		try {
			assertThat(theNode.nodeExists(pathName)).isTrue();

			/* Test the absolute path */
			String absolutePath = theNode.absolutePath();
			if (pathName != null) {
				if (!"".equals(pathName) && (theNode.parent() != null)) {
					absolutePath = absolutePath + "/" + pathName;
				}
			}
			assertThat(theNode.nodeExists(absolutePath)).isTrue();
		}
		catch (Exception e) {
			fail("node exists", e);
		}
	}

	private void testParent(Preferences theNode, String absPath) {
		try {
			String parentName = "";
			int i = absPath.lastIndexOf('/');
			if (i > 1) {
				parentName = absPath.substring(0, i);
			}
			Preferences parentNode = theNode.parent();
			if (parentNode != null) {
				assertThat(parentNode.absolutePath()).isEqualTo(parentName);
			}
			else {
				assertThat(parentName).isEmpty();
			}
		}
		catch (Exception e) {
			fail("parent", e);
		}
	}

	private void testRemove(Preferences theNode, String key) {
		try {
			theNode.remove(key);
			assertThat(Arrays.asList(theNode.keys()).contains(key)).isFalse();
		}
		catch (Exception e) {
			fail("remove key", e);
		}
	}

	private void testRemoveNode(Preferences theNode) {
		try {
			theNode.removeNode();
			assertThat(theNode.nodeExists("")).isFalse();
		}
		catch (Exception e) {
			fail("remove node", e);
		}
	}

	private void testPropValues(Preferences theNode) {
		clearKeys(theNode);
		final byte[] empty = new byte[] {};
		final byte[] trueResult = new byte[] {(byte) 0xb6, (byte) 0xbb,
				(byte) 0x9e};
		final byte[] sometextResult = new byte[] {(byte) 0xb2, (byte) 0x89,
				(byte) 0x9e, (byte) 0xb5, (byte) 0xec, (byte) 0x6d};
		assertThat(theNode.get("thekey", "(empty)")).isEqualTo("(empty)");
		assertThat(theNode.getBoolean("thekey", false)).isFalse();
		assertThat(Arrays.equals(empty, theNode.getByteArray("thekey", empty))).isTrue();
		assertThat(theNode.getDouble("thekey", 0.0d)).isCloseTo(0.0d, within(0.0d));
		assertThat(theNode.getFloat("thekey", 0.0f)).isCloseTo(0.0f, within(0.0f));
		assertThat(theNode.getInt("thekey", 0)).isZero();
		assertThat(theNode.getLong("thekey", 0l)).isZero();

		/* Test putting in a string */
		theNode.put("thekey", "astring");
		assertThat(theNode.get("thekey", "(empty)")).isEqualTo("astring");
		assertThat(theNode.getBoolean("thekey", false)).isFalse();
		assertThat(Arrays.equals(empty, theNode.getByteArray("thekey", empty))).isTrue();
		assertThat(theNode.getDouble("thekey", 0.0d)).isCloseTo(0.0d, within(0.0d));
		assertThat(theNode.getFloat("thekey", 0.0f)).isCloseTo(0.0f, within(0.0f));
		assertThat(theNode.getInt("thekey", 0)).isZero();
		assertThat(theNode.getLong("thekey", 0l)).isZero();

		/* Test putting in a boolean string */
		theNode.put("thekey", "true");
		assertThat(theNode.get("thekey", "(empty)")).isEqualTo("true");
		assertThat(theNode.getBoolean("thekey", false)).isTrue();
		assertThat(Arrays.equals(trueResult, theNode.getByteArray("thekey",
				empty))).isTrue();
		assertThat(theNode.getDouble("thekey", 0.0d)).isCloseTo(0.0d, within(0.0d));
		assertThat(theNode.getFloat("thekey", 0.0f)).isCloseTo(0.0f, within(0.0f));
		assertThat(theNode.getInt("thekey", 0)).isZero();
		assertThat(theNode.getLong("thekey", 0l)).isZero();

		/* Test putting in a boolean */
		theNode.putBoolean("thekey", true);
		assertThat(theNode.get("thekey", "(empty)")).isEqualTo("true");
		assertThat(theNode.getBoolean("thekey", false)).isTrue();
		assertThat(Arrays.equals(trueResult, theNode.getByteArray("thekey",
				empty))).isTrue();
		assertThat(theNode.getDouble("thekey", 0.0d)).isCloseTo(0.0d, within(0.0d));
		assertThat(theNode.getFloat("thekey", 0.0f)).isCloseTo(0.0f, within(0.0f));
		assertThat(theNode.getInt("thekey", 0)).isZero();
		assertThat(theNode.getLong("thekey", 0l)).isZero();

		/* Test putting in a string that works as a byte array too */
		theNode.put("thekey", "sometext");
		assertThat(theNode.get("thekey", "(empty)")).isEqualTo("sometext");
		assertThat(theNode.getBoolean("thekey", false)).isFalse();
		assertThat(Arrays.equals(sometextResult, theNode.getByteArray("thekey",
				empty))).isTrue();
		assertThat(theNode.getDouble("thekey", 0.0d)).isCloseTo(0.0d, within(0.0d));
		assertThat(theNode.getFloat("thekey", 0.0f)).isCloseTo(0.0f, within(0.0f));
		assertThat(theNode.getInt("thekey", 0)).isZero();
		assertThat(theNode.getLong("thekey", 0l)).isZero();

		/* Test putting in a double/float string */
		theNode.put("thekey", "1.0");
		assertThat(theNode.get("thekey", "(empty)")).isEqualTo("1.0");
		assertThat(theNode.getBoolean("thekey", false)).isFalse();
		assertThat(Arrays.equals(empty, theNode.getByteArray("thekey", empty))).isTrue();
		assertThat(theNode.getDouble("thekey", 0.0d)).isCloseTo(1.0d, within(0.0d));
		assertThat(theNode.getFloat("thekey", 0.0f)).isCloseTo(1.0f, within(0.0f));
		assertThat(theNode.getInt("thekey", 0)).isZero();
		assertThat(theNode.getLong("thekey", 0l)).isZero();

		/* Test putting in the Double.MIN_VALUE */
		theNode.putDouble("thekey", Double.MIN_VALUE);
		assertThat(theNode.get("thekey",
				"(empty)")).isEqualTo(Double.toString(Double.MIN_VALUE));
		assertThat(theNode.getBoolean("thekey", false)).isFalse();
		assertThat(Arrays.equals(empty, theNode.getByteArray("thekey", empty))).isTrue();
		assertThat(theNode.getDouble("thekey", 0.0d)).isCloseTo(Double.MIN_VALUE, within(0.0d));
		assertThat(theNode.getFloat("thekey", 0.0f)).isCloseTo(0.0f, within(0.0f));
		assertThat(theNode.getInt("thekey", 0)).isZero();
		assertThat(theNode.getLong("thekey", 0l)).isZero();

		/* Test putting in the Float.MIN_VALUE */
		theNode.putFloat("thekey", Float.MIN_VALUE);
		assertThat(theNode.get("thekey",
				"(empty)")).isEqualTo(Float.toString(Float.MIN_VALUE));
		assertThat(theNode.getBoolean("thekey", false)).isFalse();
		assertThat(Arrays.equals(empty, theNode.getByteArray("thekey", empty))).isTrue();
		assertThat(theNode.getDouble("thekey", 0.0d)).isCloseTo(Float.MIN_VALUE, within(0.002d));
		assertThat(theNode.getFloat("thekey", 0.0f)).isCloseTo(Float.MIN_VALUE, within(0.0f));
		assertThat(theNode.getInt("thekey", 0)).isZero();
		assertThat(theNode.getLong("thekey", 0l)).isZero();

		/* Test putting in an Integer/Long string */
		theNode.put("thekey", "1");
		assertThat(theNode.get("thekey", "(empty)")).isEqualTo("1");
		assertThat(theNode.getBoolean("thekey", false)).isFalse();
		assertThat(Arrays.equals(empty, theNode.getByteArray("thekey", empty))).isTrue();
		assertThat(theNode.getDouble("thekey", 0.0d)).isCloseTo(1.0d, within(0.0d));
		assertThat(theNode.getFloat("thekey", 0.0f)).isCloseTo(1.0f, within(0.0f));
		assertThat(theNode.getInt("thekey", 0)).isOne();
		assertThat(theNode.getLong("thekey", 0l)).isOne();

		/* Test putting in the Integer.MIN_VALUE Integer */
		theNode.putInt("thekey", Integer.MIN_VALUE);
		assertThat(theNode.get("thekey",
				"(empty)")).isEqualTo(Integer.toString(Integer.MIN_VALUE));
		assertThat(theNode.getBoolean("thekey", false)).isFalse();
		assertThat(Arrays.equals(empty, theNode.getByteArray("thekey", empty))).isTrue();
		assertThat(theNode.getDouble("thekey", 0.0d)).isCloseTo(Integer.MIN_VALUE, within(0.0d));
		assertThat(theNode.getFloat("thekey", 0.0f)).isCloseTo(Integer.MIN_VALUE, within(0.0f));
		assertThat(theNode.getInt("thekey", 0)).isEqualTo(Integer.MIN_VALUE);
		assertThat(theNode.getLong("thekey", 0l)).isEqualTo(Integer.MIN_VALUE);

		/* Test putting in the Long.MIN_VALUE */
		theNode.putLong("thekey", Long.MIN_VALUE);
		assertThat(theNode.get("thekey",
				"(empty)")).isEqualTo(Long.toString(Long.MIN_VALUE));
		assertThat(theNode.getBoolean("thekey", false)).isFalse();
		assertThat(Arrays.equals(empty, theNode.getByteArray("thekey", empty))).isTrue();
		assertThat(theNode.getDouble("thekey", 0.0d)).isCloseTo(Long.MIN_VALUE, within(0.0d));
		assertThat(theNode.getFloat("thekey", 0.0f)).isCloseTo(Long.MIN_VALUE, within(0.0f));
		assertThat(theNode.getInt("thekey", 0)).isZero();
		assertThat(theNode.getLong("thekey", 0l)).isEqualTo(Long.MIN_VALUE);
	}

	private void testIllegalNames(Preferences theNode) {
		String[] names = {"/node1/", "/node2//subnode", "node3/",
				"node4//subnode"};

		for (int i = 0; i < names.length; i++) {
			String name = names[i];
			try {
				theNode.node(name);
				fail(name);
			}
			catch (IllegalArgumentException e) {
				// expected
			}
			catch (Exception e) {
				fail(name, e);
			}
		}
	}

	private void testUnusualNames(Preferences theNode, String absPath) {
		String[] names = {"/.././_?/\\", "/ / / / ", ".././_?/\\", " / / / "};

		for (int i = 0; i < names.length; i++) {
			String name = names[i];
			String namePath = getAbsPathName(absPath, name);
			try {
				Preferences newNode = theNode.node(name);
				assertThat(newNode).isNotNull();
				assertThat(newNode.absolutePath()).isEqualTo(namePath);
			}
			catch (Exception e) {
				fail(name, e);
			}
		}
		clearChildren(theNode);
	}

	private void testCaseSensitivity(Preferences theNode) {
		/* Test if keys are case-sensitive */
		theNode.put("Name", "UPPERCASE");
		assertThat(theNode.get("name", "lowercase")).isEqualTo("lowercase");

		/* Test if node names are case-sensitive */
		theNode.node("NameOfChild");
		try {
			assertThat(theNode.nodeExists("nameofchild")).isFalse();
		}
		catch (BackingStoreException e) {
			fail("node exists error", e);
		}
		clearKeys(theNode);
		clearChildren(theNode);
	}

	/* The helper methods */
	/*
	 * These methods performs tests that may be called from multiple places
	 * (while testing different requrements. These methods may very well perform
	 * logs, but should NOT set requirement markers
	 */

	private void clearKeys(Preferences theNode) {
		try {
			theNode.clear();
			assertThat(theNode.keys()).isEmpty();
		}
		catch (Exception e) {
			fail("clear keys", e);
		}
	}

	private void clearChildren(Preferences theNode) {
		try {
			String[] children = theNode.childrenNames();
			for (int i = 0; i < children.length; i++) {
				Preferences child = theNode.node(children[i]);
				child.removeNode();
			}
			assertThat(theNode.childrenNames()).isEmpty();
		}
		catch (Exception e) {
			fail("clear children", e);
		}
	}

	private void flush(Preferences theNode) {
		try {
			theNode.flush();
		}
		catch (Exception e) {
			fail(theNode.absolutePath() + " flush failed", e);
		}
	}

	private void sync(Preferences theNode) {
		try {
			theNode.sync();
		}
		catch (Exception e) {
			fail(theNode.absolutePath() + " sync failed", e);
		}
	}
}
