/*
 * Zinc - The incremental compiler for Scala.
 * Copyright Lightbend, Inc. and Mark Harrah
 *
 * Licensed under Apache License 2.0
 * (http://www.apache.org/licenses/LICENSE-2.0).
 *
 * See the NOTICE file distributed with this work for
 * additional information regarding copyright ownership.
 */

package sbt
package internal
package inc

import xsbti.Problem
import java.io.File

import xsbti.compile.analysis.{ ReadSourceInfos, SourceInfo }

trait SourceInfos extends ReadSourceInfos {
  def ++(o: SourceInfos): SourceInfos
  def add(file: File, info: SourceInfo): SourceInfos
  def --(files: Iterable[File]): SourceInfos
  def groupBy[K](f: (File) => K): Map[K, SourceInfos]
  def allInfos: Map[File, SourceInfo]
}

object SourceInfos {
  def empty: SourceInfos = of(Map.empty)
  def of(m: Map[File, SourceInfo]): SourceInfos = new MSourceInfos(m)

  val emptyInfo: SourceInfo = makeInfo(Nil, Nil, Nil)
  def makeInfo(
      reported: Seq[Problem],
      unreported: Seq[Problem],
      mainClasses: Seq[String]
  ): SourceInfo =
    new UnderlyingSourceInfo(reported, unreported, mainClasses)
  def merge(infos: Traversable[SourceInfos]): SourceInfos = (SourceInfos.empty /: infos)(_ ++ _)
}

private final class MSourceInfos(val allInfos: Map[File, SourceInfo]) extends SourceInfos {
  def ++(o: SourceInfos) = new MSourceInfos(allInfos ++ o.allInfos)
  def --(sources: Iterable[File]) = new MSourceInfos(allInfos -- sources)
  def groupBy[K](f: File => K): Map[K, SourceInfos] = allInfos groupBy (x => f(x._1)) map { x =>
    (x._1, new MSourceInfos(x._2))
  }
  def add(file: File, info: SourceInfo) = new MSourceInfos(allInfos + ((file, info)))

  override def get(file: File): SourceInfo = allInfos.getOrElse(file, SourceInfos.emptyInfo)
  override def getAllSourceInfos: java.util.Map[File, SourceInfo] = {
    import scala.collection.JavaConverters.mapAsJavaMapConverter
    mapAsJavaMapConverter(allInfos).asJava
  }
}

private final class UnderlyingSourceInfo(
    val reportedProblems: Seq[Problem],
    val unreportedProblems: Seq[Problem],
    val mainClasses: Seq[String]
) extends SourceInfo {
  override def getReportedProblems: Array[Problem] = reportedProblems.toArray
  override def getUnreportedProblems: Array[Problem] = unreportedProblems.toArray
  override def getMainClasses: Array[String] = mainClasses.toArray
}
