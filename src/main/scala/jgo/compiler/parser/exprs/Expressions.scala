package jgo.compiler
package parser.exprs

import parser.types._
//import interm._
import interm.expr._
import interm.expr.Expr
import interm.expr.Combinators._
import interm.types._

trait Expressions extends PrimaryExprs with ExprUtils {
  lazy val expression: PPM[Expr] =                       "expression" $
    orExpr
  
  lazy val orExpr: PPM[Expr] =                "or-expression: prec 1" $
    ( orExpr ~ pos("||") ~ andExpr   ^^ or
    | andExpr
    )
  
  lazy val andExpr: PPM[Expr] =              "and-expression: prec 2" $
    ( andExpr ~ pos("&&") ~ relExpr  ^^ and
    | relExpr
    )
  
  lazy val relExpr: PPM[Expr] =       "relational expression: prec 3" $
    ( relExpr ~ pos("==") ~ addExpr  ^^ compEq
    | relExpr ~ pos("!=") ~ addExpr  ^^ compNe
    | relExpr ~ pos("<")  ~ addExpr  ^^ compLt
    | relExpr ~ pos("<=") ~ addExpr  ^^ compLeq
    | relExpr ~ pos(">")  ~ addExpr  ^^ compGt
    | relExpr ~ pos(">=") ~ addExpr  ^^ compGeq
    | addExpr
    )
  
  lazy val addExpr: PPM[Expr] =         "additive expression: prec 4" $
    ( addExpr ~ pos("+") ~ multExpr  ^^ plus
    | addExpr ~ pos("-") ~ multExpr  ^^ minus
    | addExpr ~ pos("|") ~ multExpr  ^^ bitOr
    | addExpr ~ pos("^") ~ multExpr  ^^ bitXor
    | multExpr
    )
  
  lazy val multExpr: PPM[Expr] =  "multiplicative expression: prec 5" $
    ( multExpr ~ pos("*")  ~ unaryExpr  ^^ times
    | multExpr ~ pos("/")  ~ unaryExpr  ^^ div
    | multExpr ~ pos("%")  ~ unaryExpr  ^^ mod
    | multExpr ~ pos("<<") ~ unaryExpr  ^^ shiftL
    | multExpr ~ pos(">>") ~ unaryExpr  ^^ shiftR
    | multExpr ~ pos("&")  ~ unaryExpr  ^^ bitAnd
    | multExpr ~ pos("&^") ~ unaryExpr  ^^ bitAndNot
    | unaryExpr
    )
  
  lazy val unaryExpr: PPM[Expr] =          "unary expression: prec 6" $
    ( pos("+")  ~ unaryExpr  ^^ positive
    | pos("-")  ~ unaryExpr  ^^ negative
    | pos("^")  ~ unaryExpr  ^^ bitCompl
    | pos("!")  ~ unaryExpr  ^^ Combinators.not //find out which "not" the compiler was confused with.
    | pos("<-") ~ unaryExpr  ^^ chanRecv
    | pos("&")  ~ unaryExpr  ^^ addrOf
    | pos("*")  ~ unaryExpr  ^^ deref
    | primaryExpr
    )
    
  lazy val exprList: PM[List[Expr]] =               "expression list" $
    rep1sep(expression, ",")
  
  
  /*
  private def and(e1: Expr, e2: Expr): Expr = (e1, e2) match {
    case (b1: BoolExpr, b2: BoolExpr) => And(b1, b2)
    case _ => badExpr("operand(s) of && not of boolean type")
  }
  private def or(e1: Expr, e2: Expr): Expr = (e1, e2) match {
    case (b1: BoolExpr, b2: BoolExpr) => Or(b1, b2)
    case _ => badExpr("operand(s) of || not of boolean type")
  }
  
  private def compEq(e1: Expr, e2: Expr): Expr = ifSame(e1, e2) {
    e1.t.underlying match {
      case _: NumericType => NumEquals(e1, e2)
      case BoolType       => BoolEquals(e1, e2)
      case _              => ObjEquals(e1, e2)
    }
  }
  
  private def compNe(e1: Expr, e2: Expr): Expr = ifSame(e1, e2) {
    e1.t.underlying match {
      case _: NumericType => NumNotEquals(e1, e2)
      case BoolType       => BoolNotEquals(e1, e2)
      case _              => ObjNotEquals(e1, e2)
    }
  }
  
  private def lt(e1: Expr, e2: Expr): Expr = ifSameNumericE(e1, e2)(LessThan(e1, e2))
  private def le(e1: Expr, e2: Expr): Expr = ifSameNumericE(e1, e2)(LessEquals(e1, e2))
  private def gt(e1: Expr, e2: Expr): Expr = ifSameNumericE(e1, e2)(GreaterThan(e1, e2))
  private def ge(e1: Expr, e2: Expr): Expr = ifSameNumericE(e1, e2)(GreaterEquals(e1, e2))
  
  private def plus(e1: Expr, e2: Expr): Expr =
    if (e1.t != e2.t)
      badExpr("operands have differing types %s and %s", e1.t, e2.t)
    else e1 match {
      case _ OfType (StringType)  => SimpleExpr(e1.eval |+| e2.eval |+| StrAdd, e1.t)
      case _ OfType (t: NumericType) => SimpleExpr(e1.eval |+| e2.eval |+| Add(t), e1.t)
      case _ => badExpr("operand type %s not numeric or string type", e1.t)
    }
  //Get ready for procedural abstraction, functional programming style!  //Eh.  //May 1: BLEH!! UGH!
  //[More May 1] eww.  Well, I suppose this is somewhat evocative of the kind of procedural
  //abstraction you see in idiomatic Haskell programs, which I think is called "pointfree style."
  //Wikipedia calls it "tacit programming" as well, a name in whose context all of the loud _s
  //and screaming compiler errors on missing type annotations on inferred parameters become quite
  //ironic.  Btw, if you're reading this, you've probably been following the revision history quite
  //closely!  I intend to remove all of these processing functions (and this comment along with them)
  //after implementing the equivalent ones in interm.expr.  Get ready for _monadic_ abstraction.
  private def minus(e1: Expr, e2: Expr):     Expr = ifSameNumeric(e1, e2)(encat(simple(Sub(_))))
  private def times(e1: Expr, e2: Expr):     Expr = ifSameNumeric(e1, e2)(encat(simple(Mul(_))))
  private def div(e1: Expr, e2: Expr):       Expr = ifSameNumeric(e1, e2)(encat(simple(Div(_))))
  private def mod(e1: Expr, e2: Expr):       Expr = ifSameIntegral(e1, e2)(encat(simple(Mod(_))))
  private def bitAnd(e1: Expr, e2: Expr):    Expr = ifSameIntegral(e1, e2)(encat(simple(BitwiseAnd(_))))
  private def bitAndNot(e1: Expr, e2: Expr): Expr = ifSameIntegral(e1, e2)(encat(simple(BitwiseAndNot(_))))
  private def bitOr(e1: Expr, e2: Expr):     Expr = ifSameIntegral(e1, e2)(encat(simple(BitwiseOr(_))))
  private def bitXor(e1: Expr, e2: Expr):    Expr = ifSameIntegral(e1, e2)(encat(simple(BitwiseXor(_))))
  private def shiftl(e1: Expr, e2: Expr):    Expr = ifValidShift(e1, e2)(encat(simple(ShiftL(_, _))))
  private def shiftr(e1: Expr, e2: Expr):    Expr = ifValidShift(e1, e2)(encat(simple(ShiftR(_, _))))
  
  private def pos(expr: Expr):   Expr  = ifNumeric(expr)((_, _, _) => SimpleExpr(expr.eval, expr.t))
  private def neg(expr: Expr):   Expr  = ifNumeric(expr)(simple(Neg(_)))
  private def compl(expr: Expr): Expr  = ifIntegral(expr)(simple(BitwiseCompl(_)))
//private def addrOf(expr: Expr): Expr = ifNumeric(expr)(simple(Neg(_)))
//private def deref(expr: Expr): Expr  = PtrLval(ifPtr(expr)(simple(Deref)))
  
  private def chanRecv(expr: Expr): Expr = expr match {
    case HasType(RecvChanType(t)) => SimpleExpr(expr.eval |+| ChanRecv, t)
    case HasType(t) => badExpr("operand of channel receive has type %s, which is not a receiving channel type", t)
  }
  
  private def not(expr: Expr): Expr = expr match {
    case b: BoolExpr => Not(b)
    case _ => badExpr("operand of ! has type %s; must be of boolean type", expr.t)
  }
  */
}
