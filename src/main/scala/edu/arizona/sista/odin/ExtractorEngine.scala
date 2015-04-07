package edu.arizona.sista.odin

import scala.reflect.ClassTag
import edu.arizona.sista.processors.Document
import edu.arizona.sista.odin.impl.RuleReader

class ExtractorEngine[A <: Actions : ClassTag](
    rules: String,
    actions: A = new Actions,
    globalAction: Action = ExtractorEngine.identity
) {
  val extractors = RuleReader(actions).read(rules)

  // the minimum number of iterations required for every rule to run at least once
  val minIterations = extractors.map(_.startsAt).max

  def extractFrom(document: Document, initialState: State = new State): Seq[Mention] = {
    @annotation.tailrec
    def loop(i: Int, state: State): Seq[Mention] = iteration(i, state) match {
      case Nil if i >= minIterations => state.allMentions  // we are done
      case Nil => loop(i + 1, state)
      case mentions => loop(i + 1, state.updated(globalAction(mentions, state)))
    }

    def iteration(i: Int, state: State): Seq[Mention] = for {
      extractor <- extractors
      if extractor.priority matches i
      mention <- extractor.findAllIn(document, state)
      if !state.contains(mention)
    } yield mention

    loop(1, initialState)
  }
}

object ExtractorEngine {
  /** Default global action */
  def identity(mentions: Seq[Mention], state: State): Seq[Mention] = mentions
}
