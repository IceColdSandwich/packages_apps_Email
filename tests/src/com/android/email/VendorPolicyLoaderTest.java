/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.email;

import android.content.Context;
import android.os.Bundle;
import android.test.AndroidTestCase;

import java.util.HashMap;

public class VendorPolicyLoaderTest extends AndroidTestCase {
    /**
     * Test for the case where the helper package doesn't exist.
     */
    public void testPackageNotExist() {
        VendorPolicyLoader pl = new VendorPolicyLoader(getContext(), "no.such.package",
                "no.such.Class", true);

        // getPolicy() shouldn't throw any exception.
        assertEquals(Bundle.EMPTY, pl.getPolicy(null, null));
    }

    public void testIsSystemPackage() {
        final Context c = getContext();
        assertEquals(false, VendorPolicyLoader.isSystemPackage(c, "no.such.package"));
        assertEquals(false, VendorPolicyLoader.isSystemPackage(c, "com.android.email.tests"));
        assertEquals(true, VendorPolicyLoader.isSystemPackage(c, "com.android.settings"));
    }

    /**
     * Actually call {@link VendorPolicyLoader#getPolicy}, using MockVendorPolicy as a vendor
     * policy.
     */
    public void testGetPolicy() {
        // Because MockVendorPolicy lives in a non-system apk, we need to skip the system-apk check.
        VendorPolicyLoader pl = new VendorPolicyLoader(getContext(), "com.android.email.tests",
                MockVendorPolicy.class.getName(), true);

        // Prepare result
        Bundle result = new Bundle();
        result.putInt("ret", 1);
        MockVendorPolicy.mockResult = result;

        // Arg to pass
        Bundle args = new Bundle();
        args.putString("arg1", "a");

        // Call!
        Bundle actualResult = pl.getPolicy("policy1", args);

        // Check passed args
        assertEquals("operation", "policy1", MockVendorPolicy.passedPolicy);
        assertEquals("arg", "a", MockVendorPolicy.passedBundle.getString("arg1"));

        // Check return value
        assertEquals("result", 1, actualResult.getInt("ret"));
    }


    /**
     * Same as {@link #testGetPolicy}, but with the system-apk check.  It's a test for the case
     * where we have a non-system vendor policy installed, which shouldn't be used.
     */
    public void testGetPolicyNonSystem() {
        VendorPolicyLoader pl = new VendorPolicyLoader(getContext(), "com.android.email.tests",
                MockVendorPolicy.class.getName(), false);

        MockVendorPolicy.passedPolicy = null;

        // getPolicy() shouldn't throw any exception.
        assertEquals(Bundle.EMPTY, pl.getPolicy("policy1", null));

        // MockVendorPolicy.getPolicy() shouldn't get called.
        assertNull(MockVendorPolicy.passedPolicy);
    }

    private static class MockVendorPolicy {
        public static String passedPolicy;
        public static Bundle passedBundle;
        public static Bundle mockResult;

        public static Bundle getPolicy(String operation, Bundle args) {
            passedPolicy = operation;
            passedBundle = args;
            return mockResult;
        }
    }

    /**
     * Test that any vendor policy that happens to be installed returns legal values
     * for getImapIdValues() per its API.
     * 
     * Note, in most cases very little will happen in this test, because there is
     * no vendor policy package.  Most of this test exists to test a vendor policy
     * package itself, to make sure that its API returns reasonable values.
     */
    public void testGetImapIdValues() {
        VendorPolicyLoader pl = VendorPolicyLoader.getInstance(getContext());
        String id = pl.getImapIdValues("user-name", "server.yahoo.com",
                "IMAP4rev1 STARTTLS AUTH=GSSAPI");
        // null is a reasonable result
        if (id == null) return;

        // if non-null, basic sanity checks on format
        assertEquals("\"", id.charAt(0));
        assertEquals("\"", id.charAt(id.length()-1));
        // see if we can break it up properly
        String[] elements = id.split("\"");
        assertEquals(0, elements.length % 4);
        for (int i = 0; i < elements.length; ) {
            // Because we split at quotes, we expect to find:
            // [i] = null or one or more spaces
            // [i+1] = key
            // [i+2] = one or more spaces
            // [i+3] = value
            // Here are some incomplete checks of the above
            assertTrue(elements[i] == null || elements[i].startsWith(" "));
            assertTrue(elements[i+1].charAt(0) != ' ');
            assertTrue(elements[i+2].startsWith(" "));
            assertTrue(elements[i+3].charAt(0) != ' ');
            i += 4;            
        }
    }
}