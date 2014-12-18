package org.statismo.stk.core
package common

import scala.collection.SeqView
import org.statismo.stk.core.geometry._
import org.statismo.stk.core.mesh.kdtree.KDTreeMap
import com.sun.xml.internal.bind.annotation.OverrideAnnotationOf

trait Cell {
  def pointIds: IndexedSeq[Int]
}

trait DiscreteDomain[D <: Dim] extends Domain[D] {
  def points: Iterator[Point[D]]
}

trait PointGenerator[D <: Dim] extends Function0[Point[D]]

object DiscreteDomain {
  def fromPredicateAndGenerator[D <: Dim: NDSpace](generator: PointGenerator[D], _isDefinedAt: Point[D] => Boolean) = {
    new DiscreteDomain[D] {
      override def points = Iterator.continually(generator())
      override def isDefinedAt(pt: Point[D]) = _isDefinedAt(pt)
    }
  }
}

trait FiniteDiscreteDomain[D <: Dim] extends DiscreteDomain[D] {
  def numberOfPoints: Int
}

object FiniteDiscreteDomain {

  def fromSeq[D <: Dim: NDSpace](_points: IndexedSeq[Point[D]]) =
    new FiniteDiscreteDomain[D] {
      override def points = _points.toIterator
      override def isDefinedAt(p: Point[D]) = _points.contains(p)
      override def numberOfPoints = _points.size
    }

  def fromPredicateAndGenerator[D <: Dim: NDSpace](generator: PointGenerator[D], _isDefinedAt: Point[D] => Boolean, _numberOfPoints: Int) = new FiniteDiscreteDomain[D] {
    override def points = Iterator.continually(generator()).take(_numberOfPoints)
    override def numberOfPoints = _numberOfPoints
    override def isDefinedAt(pt: Point[D]) = _isDefinedAt(pt) && points.contains(pt)
  }
}

case class SpatiallyIndexedFiniteDiscreteDomain[D <: Dim: NDSpace]  (pointSeq: IndexedSeq[Point[D]], numberOfPoints: Int) extends FiniteDiscreteDomain[D] {

  override def points = pointSeq.toIterator
  def points(idx : Int) = pointSeq(idx)
    
  private[this] lazy val kdTreeMap = KDTreeMap.fromSeq(pointSeq.zipWithIndex)
  override def isDefinedAt(pt: Point[D]) = findClosestPoint(pt)._1 == pt

  def findClosestPoint(pt: Point[D]): (Point[D], Int) = {
    val nearestPtsAndIndices = (kdTreeMap.findNearest(pt, n = 1))
    nearestPtsAndIndices(0)
  }

  def findNClosestPoints(pt: Point[D], n: Int): Seq[(Point[D], Int)] = kdTreeMap.findNearest(pt, n)
}

object SpatiallyIndexedFiniteDiscreteDomain {
  def fromSeq[D <: Dim: NDSpace](_points: IndexedSeq[Point[D]]) = SpatiallyIndexedFiniteDiscreteDomain[D](_points, _points.size)
  def fromGenereator[D <: Dim: NDSpace](generator: PointGenerator[D], _numberOfPoitns: Int) = {
    val _points = Iterator.continually(generator()).take(_numberOfPoitns).toIndexedSeq
    SpatiallyIndexedFiniteDiscreteDomain[D](_points, _numberOfPoitns)
  } 

}


