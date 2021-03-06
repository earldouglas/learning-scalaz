package day12

import scala.language.implicitConversions // eyeroll
import scala.language.higherKinds

import scalaz._
import Scalaz._

object Origami extends App {

  sealed trait List[+A]
  case object Nil extends List[Nothing]
  case class Cons[A](head: A, tail: List[A]) extends List[A]

  def wrap[A](x: A): List[A] =
    Cons(x, Nil)

  def nil[A](list: List[A]): Boolean =
    list match {
      case Nil => true
      case Cons(_, _) => false
    }

  def foldL[A,B](f: A => B => B, z: B, bs: List[A]): B =
    bs match {
      case Nil => z
      case Cons(h, t) => f(h)(foldL(f, z, t))
    }

  ////////////////////////////////////////////////////////////////////
  // Exercise 3.2

  def mapL[A,B](f: A => B, xs: List[A]): List[B] =
    xs match {
      case Nil => Nil
      case Cons(h, t) => Cons(f(h), mapL(f, t))
    }

  implicit class ListMap[A](val xs: List[A]) extends AnyVal {
    def map[B](f: A => B): List[B] = mapL(f, xs)
  }

  def appendL[A](xs: List[A], ys: List[A]): List[A] =
    xs match {
      case Nil => ys
      case Cons(h, t) => Cons(h, appendL(t, ys))
    }

  implicit class ListAppend[A](val xs: List[A]) extends AnyVal {
    def append(ys: List[A]): List[A] = appendL(xs, ys)
  }

  def concatL[A](xss: List[List[A]]): List[A] =
    xss match {
      case Nil => Nil
      case Cons(h, t) => appendL(h, concatL(t))
    }

  implicit class ListConcat[A](val xss: List[List[A]]) extends AnyVal {
    def concat: List[A] = concatL(xss)
  }

  ////////////////////////////////////////////////////////////////////
  // Exercise 3.3

  sealed trait Ordering
  case object GT extends Ordering
  case object LT extends Ordering
  case object EQ extends Ordering

  trait Ord[A] {
    def compare(x: A, y: A): Ordering
  }

  def isort[A : Ord](xs: List[A]): List[A] = {
    def insert(x: A)(xs: List[A]): List[A] =
      xs match {
        case Nil => wrap(x)
        case Cons(h, t) if implicitly[Ord[A]].compare(x, h) == LT =>
          Cons(x, Cons(h, t))
        case Cons(h, t) =>
          Cons(h, insert(x)(t))
      }
    foldL(insert, Nil, xs)
  }

  implicit object IntOrd extends Ord[Int] {
    def compare(x: Int, y: Int) =
      if (x < y) LT
      else if (x > y) GT
      else EQ
  }

  // Cons(1,Cons(2,Cons(3,Cons(4,Nil))))
  println(isort(Cons(4,Cons(3,Cons(2,Cons(1, Nil))))))

  ////////////////////////////////////////////////////////////////////
  // Unfolds

  sealed trait Maybe[+A]
  case class Just[A](x: A) extends Maybe[A]
  case object Nada extends Maybe[Nothing]

  def unfoldL[A,B](f: B => Maybe[(A,B)], z: B): List[A] =
    f(z) match {
      case Nada => Nil
      case Just((x,v)) => Cons(x, unfoldL(f,v))
    }

  trait Enum[A] {
    def succ(x: A): A
    def pred(x: A): A
  }

  def rangeNext[A: Ord: Enum](to: A)(x: A): Maybe[(A, A)] =
    if (implicitly[Ord[A]].compare(to, x) == LT) Nada
    else Just((x, implicitly[Enum[A]].succ(x)))

  def range[A: Ord: Enum](from: A, to: A): List[A] =
    unfoldL(rangeNext(to), from)

  implicit object IntEnum extends Enum[Int] {
    def succ(x: Int): Int = x + 1
    def pred(x: Int): Int = x - 1
  }

  // Cons(1,Cons(2,Cons(3,Cons(4,Cons(5,Cons(6,Cons(7,Cons(8,Cons(9,...
  println(range(1,10))

  ////////////////////////////////////////////////////////////////////
  // Unfolds using StateT

  def rangeNextStateT[A: Ord: Enum](to: A): StateT[Maybe,A,A] =
    StateT { x: A => rangeNext(to)(x) }

  def rangeStateT[A: Ord: Enum](from: A, to: A): List[A] =
    unfoldL({ x: A => rangeNextStateT(to).run(x) }, from)

  // Cons(1,Cons(2,Cons(3,Cons(4,Cons(5,Cons(6,Cons(7,Cons(8,Cons(9,...
  println(rangeStateT(1,10))

}
