package edu.arizona.sista.odin.impl

import edu.arizona.sista.struct.Interval
import edu.arizona.sista.processors.Document
import edu.arizona.sista.odin._

object TokenPattern {
  val GlobalCapture = "--GLOBAL--"

  def compile(input: String): TokenPattern = TokenPatternCompiler.compile(input)

  case class Result(
      interval: Interval,
      groups: Map[String, Seq[Interval]],
      mentions: Map[String, Seq[Mention]]
  ) {
    val start = interval.start
    val end = interval.end
  }
}

class TokenPattern(val start: Inst) {
  import TokenPattern._

  def findPrefixOf(tok: Int, sent: Int, doc: Document, state: Option[State]): Seq[Result] = {
    ThompsonVM.evaluate(start, tok, sent, doc, state) map {
      case (groups, mentions) =>
        // there must be one GlobalCapture only
        val globalCapture = groups(GlobalCapture).head
        Result(globalCapture, groups - GlobalCapture, mentions)
    }
  }

  def findFirstIn(tok: Int, sent: Int, doc: Document, state: Option[State]): Seq[Result] = {
    val n = doc.sentences(sent).size
    for (i <- tok until n) {
      val r = findPrefixOf(i, sent, doc, state)
      if (r.nonEmpty) return r
    }
    Nil
  }

  def findAllIn(tok: Int, sent: Int, doc: Document, state: Option[State]): Seq[Result] = {
    @annotation.tailrec
    def collect(i: Int, collected: Seq[Result]): Seq[Result] =
      findFirstIn(i, sent, doc, state) match {
        case Nil => collected
        case results =>
          val r = results minBy (_.interval.size)
          collect(r.end, collected ++ results)
      }
    collect(tok, Nil)
  }

  def findPrefixOf(tok: Int, sent: Int, doc: Document, state: State): Seq[Result] =
    findPrefixOf(tok, sent, doc, Some(state))

  def findPrefixOf(sent: Int, doc: Document, state: Option[State]): Seq[Result] =
    findPrefixOf(0, sent, doc, state)

  def findPrefixOf(sent: Int, doc: Document, state: State): Seq[Result] =
    findPrefixOf(sent: Int, doc: Document, Some(state))

  def findFirstIn(tok: Int, sent: Int, doc: Document, state: State): Seq[Result] =
    findFirstIn(tok, sent, doc, Some(state))

  def findFirstIn(sent: Int, doc: Document, state: Option[State]): Seq[Result] =
    findFirstIn(0, sent, doc, state)

  def findFirstIn(sent: Int, doc: Document, state: State): Seq[Result] =
    findFirstIn(sent, doc, Some(state))

  def findAllIn(tok: Int, sent: Int, doc: Document, state: State): Seq[Result] =
    findAllIn(tok, sent, doc, Some(state))

  def findAllIn(sent: Int, doc: Document, state: Option[State]): Seq[Result] =
    findAllIn(0, sent, doc, state)

  def findAllIn(sent: Int, doc: Document, state: State): Seq[Result] =
    findAllIn(sent, doc, Some(state))
}
