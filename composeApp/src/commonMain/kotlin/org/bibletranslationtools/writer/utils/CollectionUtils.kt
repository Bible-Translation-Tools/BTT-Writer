package org.bibletranslationtools.writer.utils

import java.util.SortedMap

val sortNumericallyComparator: Comparator<String> = compareBy(::getIdOrder)

fun List<String>.sortedNumerically(): List<String> = sortedBy(::getIdOrder)

fun <V> Map<String, V>.toNumericallySortedMap(): SortedMap<String, V> =
    toSortedMap(sortNumericallyComparator)


private fun getIdOrder(id: String): Int {
    // if not numeric, then will move to top of list and leave order unchanged
    return Util.strToInt(id, -1)
}