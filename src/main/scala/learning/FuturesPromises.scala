package learning

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Failure
import scala.util.Random
import scala.util.Success

object FuturesPromises extends App {

  def calculate: Int = {
    Thread.sleep(2000)
    42
  }

  val aFuture = Future {
    calculate
  }

  println(aFuture.value)

  println("Waiting for the future")
  aFuture.onComplete {
    case Success(value)     => println(value)
    case Failure(exception) => println(exception)
  }

  // NOTHING WILL HAPPEN WITHOUT THIS!
  Thread.sleep(3000)
}

object FuturesExample extends App {
  case class Profile(id: String, name: String) {
    def poke(anotherProfile: Profile) = {
      println(s"${this.name} poking ${anotherProfile.name}")
    }
  }

  object SocialNetwork {
    // database
    val names = Map(
      "fb.id.1-zuck" -> "Mark",
      "fb.id.2-bill" -> "Bill",
      "fb.id.0-dummy" -> "Dummy"
    )
    val friends = Map(
      "fb.id.1-zuck" -> "fb.id.2-bill"
    )
    val random = new Random()

    // api
    def fetchProfile(id: String): Future[Profile] = Future {
      Thread.sleep(random.nextInt(300))
      Profile(id, names(id))
    }
    def fetchBestFriend(profile: Profile): Future[Profile] = Future {
      Thread.sleep(random.nextInt(400))
      val bfId = friends(profile.id)
      Profile(bfId, names(bfId))
    }
  }

  // client: mark pokes bill
  val mark = SocialNetwork.fetchProfile("fb.id.1-zuck")
  mark.onComplete {
    case Success(markProfile) => {
      val bill = SocialNetwork.fetchBestFriend(markProfile)
      bill.onComplete {
        case Success(billProfile) => markProfile.poke(billProfile)
        case Failure(exception)   => exception.printStackTrace()
      }
    }
    case Failure(exception) => exception.printStackTrace()
  }

  Thread.sleep(2000)

  // functional composition of futures
  val nameOnTheWall = mark.map(profile => profile.name)
  val marksBestFriend =
    mark.flatMap(profile => SocialNetwork.fetchBestFriend(profile))
  val zucksBestFriendRestricted =
    marksBestFriend.filter(profile => profile.name.startsWith("Z"))

  // for-comprehensions
  for {
    mark <- SocialNetwork.fetchProfile("fb.id.1-zuck")
    bill <- SocialNetwork.fetchBestFriend(mark)
  } mark.poke(bill)

  Thread.sleep(1000)
}
