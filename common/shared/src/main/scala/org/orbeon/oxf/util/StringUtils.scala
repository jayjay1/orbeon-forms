/**
 * Copyright (C) 2016 Orbeon, Inc.
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the
 * GNU Lesser General Public License as published by the Free Software Foundation; either version
 * 2.1 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * The full text of the license is available at http://www.gnu.org/copyleft/lesser.html
 */
package org.orbeon.oxf.util

import org.orbeon.oxf.util.CoreUtils._

import scala.collection.generic.CanBuildFrom

object StringUtils {

  // Convert a string of tokens to a set
  // TODO: Move to StringOps.
  def stringToSet(s: String)               = s.splitTo[Set]()
  def stringOptionToSet(s: Option[String]) = s map stringToSet getOrElse Set.empty[String]

  //@XPathFunction
  // TODO: Move to StringOps.
  def truncateWithEllipsis(s: String, maxLength: Int, tolerance: Int): String =
    if (s.length <= maxLength + tolerance) {
      s
    } else {
      val after = s.substring(maxLength, (maxLength + tolerance + 1) min s.length)
      val afterSpaceIndex = after lastIndexOf " "
      if (afterSpaceIndex != -1)
        s.substring(0, maxLength) + after.substring(0, afterSpaceIndex) + '…'
      else
        s.substring(0, maxLength) + '…'
    }

  // Mainly for Java callers
  def trimAllToEmpty(s: String) = s.trimAllToEmpty
  def trimAllToNull(s: String)  = s.trimAllToNull
  def trimAllToOpt(s: String)   = s.trimAllToOpt

  implicit class StringOps(val s: String) extends AnyVal {

    /*
     * Rewrite in Scala of Apache StringUtils.splitWorker (http://www.apache.org/licenses/LICENSE-2.0).
     *
     * This implementation can return any collection type for which there is a builder:
     *
     * split[List]("a b")
     * split[Array]("a b")
     * split[Set]("a b")
     * split[LinkedHashSet]("a b")
     * split("a b")
     *
     * Or:
     *
     * val result: List[String]          = split("a b")(breakOut)
     * val result: Array[String]         = split("a b")(breakOut)
     * val result: Set[String]           = split("a b")(breakOut)
     * val result: LinkedHashSet[String] = split("a b")(breakOut)
     */
    def splitTo[T[_]](sep: String = null, max: Int = 0)(implicit cbf: CanBuildFrom[Nothing, String, T[String]]): T[String] = {

      val builder = cbf()

      if (s ne null) {
        val len = s.length
        if (len != 0) {
          var sizePlus1 = 1
          var i = 0
          var start = 0
          var doMatch = false

          val test: Char ⇒ Boolean =
            if (sep eq null)
              Character.isWhitespace
            else if (sep.length == 1) {
              val sepChar = sep.charAt(0)
              sepChar ==
            } else
              sep.indexOf(_) >= 0

          while (i < len) {
            if (test(s.charAt(i))) {
              if (doMatch) {
                if (sizePlus1 == max)
                  i = len

                sizePlus1 += 1

                builder += s.substring(start, i)
                doMatch = false
              }
              i += 1
              start = i
            } else {
              doMatch = true
              i += 1
            }
          }

          if (doMatch)
            builder += s.substring(start, i)
        }
      }
      builder.result()
    }

    private def isNonBreakingSpace(c: Int) =
      c == '\u00A0' || c == '\u2007' || c == '\u202F'

    private def isZeroWidthChar(c: Int) =
      c == '\u200b' || c == '\u200c' || c == '\u200d' || c == '\ufeff'

    def isBlank  = trimAllToEmpty.isEmpty
    def nonBlank = trimAllToEmpty.nonEmpty

    def trimAllToEmpty = trimControlAndAllWhitespaceToEmptyCP

    def trimAllToOpt: Option[String] = {
      val trimmed = trimAllToEmpty
      trimmed.nonEmpty option trimmed
    }

    def trimAllToNull: String = {
      val trimmed = trimAllToEmpty
      if (trimmed.isEmpty) null else trimmed
    }

    def trimControlAndAllWhitespaceToEmptyCP =
      trimToEmptyCP(c ⇒ Character.isWhitespace(c) || isNonBreakingSpace(c) || isZeroWidthChar(c) || Character.isISOControl(c))

    // Trim the string according to a matching function
    // This checks for Unicode code points, unlike String.trim() or StringUtils.trim(). When matching on spaces
    // and control characters, this is not strictly necessary since they are in the BMP and cannot collide with
    // surrogates. But in the general case it is more correct to test on code points.
    def trimToEmptyCP(matches: Int ⇒ Boolean) =
      if ((s eq null) || s.isEmpty) {
        ""
      } else {

        val it = s.iterateCodePoints

        var prefix = 0

        it takeWhile matches foreach { c ⇒
          prefix += Character.charCount(c)
        }

        var suffix = 0

        it foreach { c ⇒
          if (matches(c))
            suffix += Character.charCount(c)
          else
            suffix = 0
        }

        s.substring(prefix, s.size - suffix)
      }
  }

  private class CodePointsIterator(val cs: CharSequence) extends Iterator[Int] {

    private var nextIndex = 0

    def hasNext = nextIndex < cs.length

    def next() = {
      val result = cs.codePointAt(nextIndex)
      nextIndex += Character.charCount(result)
      result
    }
  }

  implicit class CharSequenceOps(val cs: CharSequence) extends AnyVal {

    def iterateCodePoints: Iterator[Int] = new CodePointsIterator(cs)

    def codePointAt(index: Int): Int = {
      val first = cs.charAt(index)
      if (index + 1 < cs.length) {
        if (Character.isHighSurrogate(first)) {
          val second = cs.charAt(index + 1)
          if (Character.isLowSurrogate(second))
            return Character.toCodePoint(first, second)
        }
      }
      first.toInt
    }
  }
}
