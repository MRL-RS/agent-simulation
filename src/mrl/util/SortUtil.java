package mrl.util;

import mrl.common.comparator.ConstantComparators;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.worldmodel.EntityID;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

/**
 * @author Siavash
 */
public class SortUtil {

    private static Log logger = LogFactory.getLog(SortUtil.class);

    private static int partition(Comparable arr[], int left, int right) {

        int i = left, j = right;
        Comparable tmp;
        Comparable pivot = arr[(left + right) / 2];

        while (i <= j) {

            while (arr[i].compareTo(pivot) < 0) {
                i++;
            }
            while (arr[j].compareTo(pivot) > 0) {
                j--;
            }
            if (i <= j) {
                tmp = arr[i];
                arr[i] = arr[j];
                arr[j] = tmp;
                i++;
                j--;
            }
        }
        return i;
    }

    private static int partition(List<Comparable> arr, int left, int right) {

        int i = left, j = right;
        Comparable tmp;
        Comparable pivot = arr.get((left + right) / 2);

        while (i <= j) {
            while (arr.get(i).compareTo(pivot) < 0) {
                i++;
            }
            while (arr.get(j).compareTo(pivot) > 0) {
                j--;
            }
            if (i <= j) {
                tmp = arr.get(i);
                arr.set(i, arr.get(j));
                arr.set(j, tmp);
                i++;
                j--;
            }
        }
        return i;
    }

    private static int partition(List<Object> arr, int left, int right, Method comparedMethod) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        int i = left;
        int j = right;
        Object tmp;
        Object pivot = arr.get((left + right) / 2);
        Comparable pivotComparable = (Comparable) comparedMethod.invoke(pivot);
        while (i <= j) {
            Comparable leftComparable = (Comparable) comparedMethod.invoke(arr.get(i));
            while (leftComparable.compareTo(pivotComparable) < 0) {
                i++;
                leftComparable = (Comparable) comparedMethod.invoke(arr.get(i));
            }
            Comparable rightComparable = (Comparable) comparedMethod.invoke(arr.get(j));
            while (rightComparable.compareTo(pivotComparable) > 0) {
                j--;
                rightComparable = (Comparable) comparedMethod.invoke(arr.get(j));
            }
            if (i <= j) {
                tmp = arr.get(i);
                arr.set(i, arr.get(j));
                arr.set(j, tmp);
                i++;
                j--;
            }
        }
        return i;
    }

    private static void quickSort(List<Comparable> arr, int left, int right) {
        int index = partition(arr, left, right);

        if (left < index - 1) {
            quickSort(arr, left, index - 1);
        }
        if (index < right) {
            quickSort(arr, index, right);
        }
    }

    private static void quickSort(List<Object> arr, int left, int right, Method comparedMethod) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        int index = partition(arr, left, right, comparedMethod);
        if (left < index - 1) {
            quickSort(arr, left, index - 1, comparedMethod);
        }
        if (index < right) {
            quickSort(arr, index, right, comparedMethod);
        }
    }

    /**
     * Sorts a list of comparable objects.
     *
     * @param list ast.
     */
    public static void sort(List<Comparable> list) {
        quickSort(list, 0, list.size() - 1);
    }

    public static void sort(List<Object> list, String comparedMethodName) throws NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        Method comparedMethod = list.get(0).getClass().getMethod(comparedMethodName);
        quickSort(list, 0, list.size() - 1, comparedMethod);
    }

    public static <T> LinkedHashMap<Comparable, T> sort(Map<Comparable, T> map) {
        LinkedHashMap<Comparable, T> sortedMap = new LinkedHashMap<Comparable, T>();
        Collection<Comparable> keyCollection = map.keySet();
        List<Comparable> orderedKeyList = new ArrayList<Comparable>();
        orderedKeyList.addAll(keyCollection);
        quickSort(orderedKeyList, 0, orderedKeyList.size() - 1);

        for (Comparable keyItem : orderedKeyList) {
            sortedMap.put(keyItem,
                    map.get(keyItem));
        }

        return sortedMap;
    }

    public static <T> LinkedHashMap<Object, T> sort(Map<Object, T> map, String comparedMethodName) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException {
        LinkedHashMap<Object, T> sortedMap = new LinkedHashMap<Object, T>();
        Collection<Object> keyCollection = map.keySet();
        List<Object> orderedKeyList = new ArrayList<Object>();
        orderedKeyList.addAll(keyCollection);
        Method m = orderedKeyList.get(0).getClass().getMethod(comparedMethodName);
        quickSort(orderedKeyList, 0, orderedKeyList.size() - 1, m);

        for (Object keyItem : orderedKeyList) {
            sortedMap.put(keyItem,
                    map.get(keyItem));
        }

        return sortedMap;
    }

    public static void sortByID(List<? extends StandardEntity> inp) {
        Collections.sort(inp, ConstantComparators.ID_COMPARATOR);
    }

    public static void sortByEntityID(List<? extends EntityID> inp) {
        Collections.sort(inp, ConstantComparators.EntityID_COMPARATOR);
    }

    public static void sortByValue(List<? extends Integer> inp) {
        Collections.sort(inp, ConstantComparators.VALUE_COMPARATOR);
    }


}
