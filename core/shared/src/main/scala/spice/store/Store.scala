package spice.store

trait Store {
  def get[T](key: String): Option[T]

  def update[T](key: String, value: T): Unit

  def remove(key: String): Unit

  def apply[T](key: String): T = get[T](key).getOrElse(throw new NullPointerException(s"$key not found."))

  def getOrElse[T](key: String, default: => T): T = get[T](key).getOrElse(default)

  def getOrSet[T](key: String, default: => T): T = synchronized {
    get[T](key) match {
      case Some(value) => value
      case None =>
        val value: T = default
        update(key, value)
        value
    }
  }
}