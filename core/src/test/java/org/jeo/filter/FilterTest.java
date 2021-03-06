/* Copyright 2013 The jeo project. All rights reserved.
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
package org.jeo.filter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.jeo.feature.BasicFeature;
import org.jeo.feature.Feature;
import org.jeo.filter.cql.CQL;
import org.junit.Test;

public class FilterTest {
    
    @Test
    public void testLiteral() {
        Literal l = new Literal(12);
        assertEquals(Integer.valueOf(12), l.evaluate(null));
    }

    @Test
    public void testProperty() {
        Map<String,Object> map = new HashMap<String, Object>();
        map.put("foo", "bar");

        Property p = new Property("foo");
        assertEquals("bar", p.evaluate(new BasicFeature(null, map)));
    }

    @Test
    public void testComparison() {
        Map<String,Object> map = new HashMap<String, Object>();
        map.put("str", "one");
        map.put("int", 1);

        Feature f = new BasicFeature(null, map);

        Comparison c = new Comparison(Comparison.Type.EQUAL, 
            new Property("str"), new Literal("one"));
        assertTrue(c.apply(f));

        c = new Comparison(Comparison.Type.EQUAL, 
                new Property("int"), new Literal(2));
        assertFalse(c.apply(f));

        c = new Comparison(Comparison.Type.LESS, 
                new Property("int"), new Literal(2));
        assertTrue(c.apply(f));

        c = new Comparison(Comparison.Type.LESS_OR_EQUAL, 
                new Property("int"), new Literal(1));
        assertTrue(c.apply(f));

        c = new Comparison(Comparison.Type.GREATER_OR_EQUAL, 
                new Property("int"), new Literal(1));
        assertTrue(c.apply(f));

        c = new Comparison(Comparison.Type.GREATER, 
                new Property("int"), new Literal(1));
        assertFalse(c.apply(f));
    }

    @Test
    public void testComparisonConversion() {
        assertTrue(
            new Comparison(Comparison.Type.EQUAL, new Literal(1), new Literal(1.0)).apply(null));
        assertTrue(
            new Comparison(Comparison.Type.EQUAL, new Literal("1"), new Literal(1)).apply(null));
        assertTrue(
            new Comparison(Comparison.Type.EQUAL, new Literal(1.0), new Literal("1")).apply(null));
    }

    @Test
    public void testLogic() {
        Map<String,Object> map = new HashMap<String, Object>();
        map.put("str", "one");
        map.put("int", 1);

        Feature f = new BasicFeature(null, map);

        Comparison c1 = new Comparison(Comparison.Type.EQUAL, 
                new Property("str"), new Literal("one"));
        Comparison c2 = new Comparison(Comparison.Type.EQUAL, 
                new Property("int"), new Literal(1));

        Logic l = new Logic(Logic.Type.AND, c1, c2);
        assertTrue(l.apply(f));
    }

    @Test
    public void testIn() {
        Map<String,Object> map = new HashMap<String, Object>();
        map.put("str", "one");
        map.put("int", 1);

        Feature f = new BasicFeature(null, map);

        assertTrue(new In(new Property("str"), Arrays.asList(new Literal("one")), false).apply(f));
        assertTrue(new In(new Property("str"), Arrays.asList(new Literal("two"), new Literal("one")), false).apply(f));

        assertFalse(new In(new Property("str"), Arrays.asList(new Literal("one")), true).apply(f));
        assertFalse(new In(new Property("str"), Arrays.asList(new Literal("two")), false).apply(f));
    }

    @Test
    public void testLike() {
        Map<String,Object> map = new HashMap<String, Object>();
        map.put("name", "abcdef");
        map.put("num", 123456);

        Feature f = new BasicFeature(null, map);
        assertTrue(new Like(new Property("name"), new Literal("%cd%"), false).apply(f));
        assertFalse(new Like(new Property("name"), new Literal("%cd"), false).apply(f));
        assertFalse(new Like(new Property("name"), new Literal("cd%"), false).apply(f));
        assertTrue(new Like(new Property("name"), new Literal("cd%"), true).apply(f));

        assertTrue(new Like(new Property("num"), new Literal("123%"), false).apply(f));
    }

    @Test
    public void testNull() {
        Map<String,Object> map = new HashMap<String, Object>();
        map.put("x", null);
        map.put("y", 1);

        Feature f = new BasicFeature(null, map);
        assertTrue(new Null("x", false).apply(f));
        assertFalse(new Null("y", false).apply(f));
        assertFalse(new Null("x", true).apply(f));
        assertTrue(new Null("y", true).apply(f));
        // missing properties always short-cut to false
        assertFalse(new Null("z", false).apply(f));
        assertFalse(new Null("z", true).apply(f));
    }

    @Test
    public void testMissingProperty() throws Exception {
        // verify the short-cut-false behavior for filters that reference a
        // missing property: 'y' in this case
        Map<String,Object> map = new HashMap<String, Object>();
        map.put("x", 5);

        Feature f = new BasicFeature(null, map);

        // comparison
        assertFalse(CQL.parse("y < 5").apply(f));
        assertFalse(CQL.parse("y = 5").apply(f));
        assertFalse(CQL.parse("5 > y").apply(f));
        assertFalse(CQL.parse("5 = y").apply(f));

        // others
        assertFalse(CQL.parse("y in (5)").apply(f));
        assertFalse(CQL.parse("y like '5'").apply(f));
        assertFalse(CQL.parse("contains(y, POINT(0 0))").apply(f));
        assertFalse(CQL.parse("y is null").apply(f));
        assertFalse(CQL.parse("in (5)").apply(f));

        // math
        assertFalse(CQL.parse("y < x + 5").apply(f));
        assertFalse(CQL.parse("y + 5 < x").apply(f));

        // logic
        assertTrue(CQL.parse("y > 5 or x = 5").apply(f));
        assertTrue(CQL.parse("x = 5 or y > 5").apply(f));
    }
}
