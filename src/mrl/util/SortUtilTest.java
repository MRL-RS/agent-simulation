package mrl.util;
//
//import java.lang.reflect.Method;
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.Iterator;
//import java.util.LinkedHashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.Random;
//import org.apache.commons.logging.Log;
//import org.apache.commons.logging.LogFactory;
////import org.junit.After;
////import org.junit.AfterClass;
////import org.junit.Before;
////import org.junit.BeforeClass;
////import org.junit.Test;
////import static org.junit.Assert.*;
//

/**
 * @author Siavash
 */
public class SortUtilTest {
//
//    private static Log logger = LogFactory.getLog(SortUtilTest.class);
//    private static final int SUBJECT_LENGTH = 100000;
//
//    public SortUtilTest() {
//    }
//
//    @BeforeClass
//    public static void setUpClass() throws Exception {
//    }
//
//    @AfterClass
//    public static void tearDownClass() throws Exception {
//    }
//
//    @Before
//    public void setUp() {
//    }
//
//    @After
//    public void tearDown() {
//    }
//
//    private List<Comparable> getRandomComparableList() {
//        List<Comparable> list = new ArrayList<Comparable>();
//        Random r = new Random(System.currentTimeMillis());
//        String s = "abcdefghijklmnopqrstuvwxyz";
//        String randomString = "";
//        for (int i = 0; i < SUBJECT_LENGTH; i++) {
//            for (int j = 0; j < 10; j++) {
//                randomString += s.charAt(r.nextInt(26));
//            }
//            list.add(randomString);
//            randomString = "";
//        }
//        return list;
//    }
//
//    private List<Object> getRandomObjectList() {
//        List<Object> list = new ArrayList<Object>();
//        for (int i = 0; i < SUBJECT_LENGTH; i++) {
//            list.add(new TestObject());
//        }
//        return list;
//    }
//
//    private Map<Comparable, Object> getRandomComparableMap() {
//        Map<Comparable, Object> map = new HashMap<Comparable, Object>();
//        for (int i = 0; i < SUBJECT_LENGTH; i++) {
//            map.put(new TestObject().toString(), new TestObject());
//        }
//        return map;
//    }
//
//    private Map<Object, Object> getRandomObjectMap() {
//        Map<Object, Object> map = new HashMap<Object, Object>();
//        for (int i = 0; i < SUBJECT_LENGTH; i++) {
//            map.put(new TestObject(), new TestObject());
//        }
//        return map;
//    }
//
//    /**
//     * Test of sort method, of class SortUtil.
//     */
//    @Test
//    public void testSortList() {
//        long startTime = System.nanoTime();
//        List<Comparable> list = getRandomComparableList();
//        SortUtil.sort(list);
//
//        Comparable cmp = list.get(0);
//        for (Comparable c : list) {
//            if (cmp.compareTo(c) > 0) {
//                fail("Sort failed.");
//            }
//            cmp = c;
//        }
//        long endTime = System.nanoTime();
//        float elapsedMillis = (endTime - startTime) / 1000000;
//        logger.info("Elapsed : " + elapsedMillis + "ms for sorting : " + SUBJECT_LENGTH + " objects.");
//    }
//
//    /**
//     * Test of sort method, of class SortUtil.
//     */
//    @Test
//    public void testSortListString() {
//        long startTime = System.nanoTime();
//        try {
//            List<Object> list = getRandomObjectList();
//            String comparedMethodName = "toString";
//            SortUtil.sort(list, comparedMethodName);
//            Method m = TestObject.class.getMethod(comparedMethodName);
//            Comparable cmp = (Comparable) m.invoke(list.get(0));
//            //logger.debug(cmp);
//            for (Object object : list) {
//                Comparable c = (Comparable) m.invoke(object);
//                if (cmp.compareTo(c) > 0) {
//                    fail("Sort failed.");
//                }
//                cmp = c;
//            }
//        } catch (Exception e) {
//            logger.error("Sort Failed.");
//            logger.debug("Stack trace:", e);
//            fail("Sort failed.");
//        }
//        long endTime = System.nanoTime();
//        float elapsedMillis = (endTime - startTime) / 1000000;
//        logger.info("Elapsed : " + elapsedMillis + "ms for sorting : " + SUBJECT_LENGTH + " objects.");
//    }
//
//    /**
//     * Test of sort method, of class SortUtil.
//     */
//    @Test
//    public void testSortMap() {
//        Map<Comparable, Object> map = getRandomComparableMap();
//        LinkedHashMap result = SortUtil.sort(map);
//        Comparable cmp = (Comparable) result.keySet().iterator().next();
//
//
//        for (Iterator i = result.keySet().iterator(); i.hasNext();) {
//            Comparable c = (Comparable) i.next();
//            if (cmp.compareTo(c) > 0) {
//                fail("Sort failed.");
//            }
//            cmp = c;
//        }
//
//    }
//
//    /**
//     * Test of sort method, of class SortUtil.
//     */
//    @Test
//    public void testSortMapString() throws Exception {
//        Map<Object, Object> map = getRandomObjectMap();
//        String comparedMethodName = "toString";
//        LinkedHashMap result = SortUtil.sort(map, comparedMethodName);
//        Method m = result.keySet().iterator().next().getClass().getMethod(comparedMethodName);
//        Comparable cmp = (Comparable) m.invoke(result.keySet().iterator().next());
//
//
//        for (Iterator i = result.keySet().iterator(); i.hasNext();) {
//            Comparable c = (Comparable) m.invoke(i.next());
//            if (cmp.compareTo(c) > 0) {
//                fail("Sort failed.");
//            }
//            cmp = c;
//        }
//    }
//
//    private class TestObject {
//
//        private String internalValue;
//
//        public TestObject() {
//            String s = "abcdefghijklmnopqrstuvwxyz";
//            String randomString = "";
//            for (int j = 0; j < 10; j++) {
//                randomString += s.charAt((int) (Math.random() * 26));
//            }
//            internalValue = randomString;
//        }
//
//        @Override
//        public String toString() {
//            return internalValue;
//        }
//    }
}
