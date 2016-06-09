/*
 * Copyright 2016 Dennis Vriend
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
 */

package com.github.dnvriend.query

import akka.persistence.query.EventEnvelope
import com.github.dnvriend.TestSpec
import org.scalatest.Ignore

@Ignore
class CurrentEventsByTagTest extends TestSpec {

  it should "not find an event by tag for unknown tag" in {
    withTestActors() { (actor1, actor2, actor3) ⇒
      actor1 ! withTags(1, "one")
      actor2 ! withTags(2, "two")
      actor3 ! withTags(3, "three")

      eventually {
        countJournal shouldBe 3
      }

      withCurrentEventsByTag()("unknown", 0) { tp ⇒
        tp.request(Int.MaxValue)
        tp.expectComplete()
      }
    }
  }

  it should "find all events by tag" in {
    withTestActors() { (actor1, actor2, actor3) ⇒
      actor1 ! withTags(1, "number")
      actor2 ! withTags(2, "number")
      actor3 ! withTags(3, "number")

      eventually {
        countJournal shouldBe 3
      }

      withCurrentEventsByTag()("number", 0) { tp ⇒
        tp.request(Int.MaxValue)
        tp.expectNextPF { case EventEnvelope(1, _, _, _) ⇒ }
        tp.expectNextPF { case EventEnvelope(2, _, _, _) ⇒ }
        tp.expectNextPF { case EventEnvelope(3, _, _, _) ⇒ }
        tp.expectComplete()
      }
    }
  }

  it should "find events from an offset" in {
    withTestActors() { (actor1, actor2, actor3) ⇒
      actor1 ! withTags(1, "number2")
      actor2 ! withTags(2, "number2")
      actor3 ! withTags(3, "number2")

      eventually {
        countJournal shouldBe 3
      }

      withCurrentEventsByTag()("number2", 2) { tp ⇒
        tp.request(Int.MaxValue)
        tp.expectNextPF { case EventEnvelope(3, _, _, _) ⇒ }
        tp.expectComplete()
      }
    }
  }

  it should "persist and find a tagged event with one tag" in
    withTestActors() { (actor1, actor2, actor3) ⇒
      actor1 ! withTags(1, "one2")

      withClue("query should find the event by tag") {
        withCurrentEventsByTag()("one2", 0) { tp ⇒
          tp.request(Int.MaxValue)
          tp.expectNextPF { case EventEnvelope(1, _, _, _) ⇒ }
          tp.expectComplete()
        }
      }

      withClue("query should find the event by persistenceId") {
        withCurrentEventsByPersistenceId()("my-1", 1, 1) { tp ⇒
          tp.request(Int.MaxValue)
          tp.expectNextPF { case EventEnvelope(1, _, _, _) ⇒ }
          tp.expectComplete()
        }
      }
    }

  it should "persist and find a tagged event with multiple tags" in
    withTestActors() { (actor1, actor2, actor3) ⇒
      withClue("Persisting multiple tagged events") {
        actor1 ! withTags(1, "one", "1", "prime")
        actor1 ! withTags(2, "two", "2", "prime")
        actor1 ! withTags(3, "three", "3", "prime")
        actor1 ! withTags(4, "four", "4")
        actor1 ! withTags(5, "five", "5", "prime")

        actor2 ! withTags(3, "three", "3", "prime")
        actor3 ! withTags(3, "three", "3", "prime")

        actor1 ! 1
        actor1 ! 1

        eventually {
          countJournal shouldBe 9
        }
      }

      withClue("query should find events for tag 'one'") {
        withCurrentEventsByTag()("one", 0) { tp ⇒
          tp.request(Int.MaxValue)
          tp.expectNextPF { case EventEnvelope(1, _, _, _) ⇒ }
          tp.expectComplete()
        }
      }

      withClue("query should find events for tag 'prime'") {
        withCurrentEventsByTag()("prime", 0) { tp ⇒
          tp.request(Int.MaxValue)
          tp.expectNextPF { case EventEnvelope(1, _, _, _) ⇒ }
          tp.expectNextPF { case EventEnvelope(2, _, _, _) ⇒ }
          tp.expectNextPF { case EventEnvelope(3, _, _, _) ⇒ }
          tp.expectNextPF { case EventEnvelope(4, _, _, _) ⇒ }
          tp.expectNextPF { case EventEnvelope(5, _, _, _) ⇒ }
          tp.expectNextPF { case EventEnvelope(6, _, _, _) ⇒ }
          tp.expectComplete()
        }
      }

      withClue("query should find events for tag '3'") {
        withCurrentEventsByTag()("3", 0) { tp ⇒
          tp.request(Int.MaxValue)
          tp.expectNextPF { case EventEnvelope(1, _, _, _) ⇒ }
          tp.expectNextPF { case EventEnvelope(2, _, _, _) ⇒ }
          tp.expectNextPF { case EventEnvelope(3, _, _, _) ⇒ }
          tp.expectComplete()
        }
      }

      withClue("query should find events for tag '3'") {
        withCurrentEventsByTag()("4", 0) { tp ⇒
          tp.request(Int.MaxValue)
          tp.expectNextPF { case EventEnvelope(1, _, _, _) ⇒ }
          tp.expectComplete()
        }
      }
    }
}
