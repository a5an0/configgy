package net.lag.configgy

import scala.collection.mutable.Stack
import scala.util.parsing.combinator._
import scala.util.parsing.combinator.syntactical.TokenParsers


/**
 * An exception thrown when parsing a config file, if there was an error
 * during parsing. The <code>reason</code> string will contain the parsing
 * error details.
 */
class ParseException(reason: String) extends Exception(reason)


private[configgy] class ConfigParser(var attr: Attributes, val importer: Importer) extends TokenParsers {
    type Tokens = ConfigLexer
    val lexical = new Tokens
    
    import lexical.{Assign, CloseTag, Delim, Ident, Keyword, Number, OpenTag, QuotedString}
    
    val sections = new Stack[String]
    var prefix = ""
    
    
    def root = rep(includeFile | assignment | toggle | sectionOpen | sectionClose)
    
    def value = number | string | stringList
    def number = accept("number", { case Number(x) => if (x.contains('.')) x else Integer.parseInt(x) })
    def string = accept("string", { case QuotedString(x) => attr.interpolate(prefix, x) })
    def stringList = accept(Delim("[")) ~ repsep(string, Delim(",")) ~ accept(Delim("]")) ^^ { list => list.toArray }
    
    def assignment = assignName ~ accept("operation", { case Assign(x) => x }) ~ value ^^ {
        case k ~ a ~ v => if (a match {
            case "=" => true
            case "?=" => ! attr.contains(prefix + k)
        }) v match {
            case x: Int => attr(prefix + k) = x
            case x: String => attr(prefix + k) = x
            case x: Array[String] => attr(prefix + k) = x
        }
    }
    def toggle = assignName ~ onoff ^^ { case k ~ v => attr(k) = v } 
    def assignName = accept("key", { case Ident(x) => x })
    def onoff = accept("on/off", {
        case Ident("on") => true
        case Ident("off") => false
    })

    def sectionOpen = accept("open tag", { case OpenTag(x) => x }) ^^ {
        case x: String => {
            sections += x
            prefix = sections.mkString("", ".", ".")
        }
    }
    
    def sectionClose = accept("close tag", { case CloseTag(x) => x }) ^^ {
        case x: String => {
            if (sections.isEmpty) {
                fail("dangling close tag: " + x)
            } else {
                val last = sections.pop
                if (last != x) {
                    fail("got closing tag for " + x + ", expected " + last)
                } else {
                    prefix = sections.mkString(".")
                }
            }
        }
    }
    
    def includeFile = accept(Keyword("include")) ~ string ^^ {
        case filename: String => {
            new ConfigParser(attr.makeAttributes(sections.mkString(".")), importer) parse importer.importFile(filename)
        }
    }
    
    def parse(in: String): Unit = {
        phrase(root)(new lexical.Scanner(in)) match {
            case Success(result, _) => result
            case x @ Failure(msg, _) => throw new ParseException(x.toString)
            case x @ Error(msg, _) => throw new ParseException(x.toString)
        }
    }
}