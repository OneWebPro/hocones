package pl.onewebpro.hocones.md.document.md

import net.steppschuh.markdowngenerator.text.TextBuilder
import pl.onewebpro.hocones.parser.entity.HoconResolvedReference

trait HoconResolvedReferenceOps {
  self: DocumentToMdGenerator[_] =>

  implicit class ResolvedReferenceBuilderOps(builder: TextBuilder) extends DefaultValue {
    def referenceTo(reference: HoconResolvedReference): TextBuilder =
      builder.label("Reference to:").text(reference.reference.result.name).newParagraph()

    def defaultValue(reference: HoconResolvedReference): TextBuilder =
      extractDefaultValue(reference.value).fold(builder) {
        value => builder.label("Value:").text(value).newParagraph()
      }
  }

}