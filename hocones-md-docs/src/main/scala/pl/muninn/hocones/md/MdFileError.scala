package pl.muninn.hocones.md

case class MdFileError(message: String, cause: Throwable = None.orNull) extends Error(message, cause)
