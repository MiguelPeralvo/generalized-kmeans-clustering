/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * This code is a modified version of the original Spark 1.0.2 K-Means implementation.
 */

package com.rincaro.clusterer

import com.massivedatascience.clusterer.KMeans

import scala.util.Random

import org.scalatest.FunSuite

import org.apache.spark.mllib.linalg.{Vector, Vectors}

import com.massivedatascience.clusterer.TestingUtils._

class KMeansSuite extends FunSuite with LocalSparkContext {

  import com.massivedatascience.clusterer.KMeans._


  test("single cluster") {
    val data = sc.parallelize(Array(
      Vectors.dense(1.0, 2.0, 6.0),
      Vectors.dense(1.0, 3.0, 0.0),
      Vectors.dense(1.0, 4.0, 6.0)
    ))

    val center = Vectors.dense(1.0, 3.0, 4.0)

    // No matter how many runs or iterations we use, we should get one cluster,
    // centered at the mean of the points

    var model = KMeans.train(data, k = 1, maxIterations = 1)
    assert(model.clusterCenters.head ~== center absTol 1E-5)

    model = KMeans.train(data, k = 1, maxIterations = 2)
    assert(model.clusterCenters.head ~== center absTol 1E-5)

    model = KMeans.train(data, k = 1, maxIterations = 5)
    assert(model.clusterCenters.head ~== center absTol 1E-5)

    model = KMeans.train(data, k = 1, maxIterations = 1, runs = 5)
    assert(model.clusterCenters.head ~== center absTol 1E-5)

    model = KMeans.train(data, k = 1, maxIterations = 1, runs = 5)
    assert(model.clusterCenters.head ~== center absTol 1E-5)

    model = KMeans.train(data, k = 1, maxIterations = 1, runs = 1, initializerName = RANDOM)
    assert(model.clusterCenters.head ~== center absTol 1E-5)

    model = KMeans.train(
      data, k = 1, maxIterations = 1, runs = 1, initializerName = K_MEANS_PARALLEL)
    assert(model.clusterCenters.head ~== center absTol 1E-5)
  }

  test("no distinct points") {
    val data = sc.parallelize(
      Array(
        Vectors.dense(1.0, 2.0, 3.0),
        Vectors.dense(1.0, 2.0, 3.0),
        Vectors.dense(1.0, 2.0, 3.0)),
      2)
    val center = Vectors.dense(1.0, 2.0, 3.0)

    // Make sure code runs.
    var model = KMeans.train(data, k = 2, maxIterations = 1)
    assert(model.clusterCenters.size === 1)
  }

  test("more clusters than points") {
    val data = sc.parallelize(
      Array(
        Vectors.dense(1.0, 2.0, 3.0),
        Vectors.dense(1.0, 3.0, 4.0)),
      2)

    // Make sure code runs.
    var model = KMeans.train(data, k = 3, maxIterations = 1)
    assert(model.clusterCenters.size === 2, s"${model.clusterCenters.size} != 2")
  }

  test("single cluster with big dataset") {
    val smallData = Array(
      Vectors.dense(1.0, 2.0, 6.0),
      Vectors.dense(1.0, 3.0, 0.0),
      Vectors.dense(1.0, 4.0, 6.0)
    )
    val data = sc.parallelize((1 to 100).flatMap(_ => smallData), 4)

    // No matter how many runs or iterations we use, we should get one cluster,
    // centered at the mean of the points

    val center = Vectors.dense(1.0, 3.0, 4.0)

    var model = KMeans.train(data, k = 1, maxIterations = 1)
    assert(model.clusterCenters.size === 1)
    assert(model.clusterCenters.head ~== center absTol 1E-5)

    model = KMeans.train(data, k = 1, maxIterations = 2)
    assert(model.clusterCenters.head ~== center absTol 1E-5)

    model = KMeans.train(data, k = 1, maxIterations = 5)
    assert(model.clusterCenters.head ~== center absTol 1E-5)

    model = KMeans.train(data, k = 1, maxIterations = 1, runs = 5)
    assert(model.clusterCenters.head ~== center absTol 1E-5)

    model = KMeans.train(data, k = 1, maxIterations = 1, runs = 5)
    assert(model.clusterCenters.head ~== center absTol 1E-5)

    model = KMeans.train(data, k = 1, maxIterations = 1, runs = 1, initializerName = RANDOM)
    assert(model.clusterCenters.head ~== center absTol 1E-5)

    model = KMeans.train(data, k = 1, maxIterations = 1, runs = 1, initializerName = K_MEANS_PARALLEL)
    assert(model.clusterCenters.head ~== center absTol 1E-5)
  }

  test("single cluster with sparse data") {

    val n = 10000
    val data = sc.parallelize((1 to 100).flatMap { i =>
      val x = i / 1000.0
      Array(
        Vectors.sparse(n, Seq((0, 1.0 + x), (1, 2.0), (2, 6.0))),
        Vectors.sparse(n, Seq((0, 1.0 - x), (1, 2.0), (2, 6.0))),
        Vectors.sparse(n, Seq((0, 1.0), (1, 3.0 + x))),
        Vectors.sparse(n, Seq((0, 1.0), (1, 3.0 - x))),
        Vectors.sparse(n, Seq((0, 1.0), (1, 4.0), (2, 6.0 + x))),
        Vectors.sparse(n, Seq((0, 1.0), (1, 4.0), (2, 6.0 - x)))
      )
    }, 4)


    data.persist()

    // No matter how many runs or iterations we use, we should get one cluster,
    // centered at the mean of the points

    val center = Vectors.sparse(n, Seq((0, 1.0), (1, 3.0), (2, 4.0)))

    var model = KMeans.train(data, k = 1, maxIterations = 1)
    assert(model.clusterCenters.head ~== center absTol 1E-5)

    model = KMeans.train(data, k = 1, maxIterations = 2)
    assert(model.clusterCenters.head ~== center absTol 1E-5)

    model = KMeans.train(data, k = 1, maxIterations = 5)
    assert(model.clusterCenters.head ~== center absTol 1E-5)

    model = KMeans.train(data, k = 1, maxIterations = 1, runs = 5)
    assert(model.clusterCenters.head ~== center absTol 1E-5)

    model = KMeans.train(data, k = 1, maxIterations = 1, runs = 5)
    assert(model.clusterCenters.head ~== center absTol 1E-5)

    model = KMeans.train(data, k = 1, maxIterations = 1, runs = 1, initializerName = RANDOM)
    assert(model.clusterCenters.head ~== center absTol 1E-5)

    model = KMeans.train(data, k = 1, maxIterations = 1, runs = 1, initializerName = K_MEANS_PARALLEL)
    assert(model.clusterCenters.head ~== center absTol 1E-5)

    model = KMeans.train(data, k = 1, maxIterations = 1, runs = 1, initializerName = K_MEANS_PARALLEL,
      distanceFunctionName = RELATIVE_ENTROPY)
    assert(model.clusterCenters.head ~== center absTol 1E-5)

    model = KMeans.train(data, k = 1, maxIterations = 1, runs = 1, initializerName = K_MEANS_PARALLEL,
      distanceFunctionName = EUCLIDEAN)
    assert(model.clusterCenters.head ~== center absTol 1E-5)


    model = KMeans.train(data, k = 1, maxIterations = 1, runs = 1, initializerName = K_MEANS_PARALLEL,
      distanceFunctionName = SPARSE_EUCLIDEAN)
    assert(model.clusterCenters.head ~== center absTol 1E-5)


    model = KMeans.train(data, k = 1, maxIterations = 1, runs = 1, initializerName = K_MEANS_PARALLEL,
      distanceFunctionName = DISCRETE_KL)
    assert(model.clusterCenters.head ~== center absTol 1E-5)


    model = KMeans.train(data, k = 1, maxIterations = 1, runs = 1, initializerName = K_MEANS_PARALLEL,
      distanceFunctionName = SPARSE_SMOOTHED_KL)
    assert(model.clusterCenters.head ~== center absTol 1E-5)

    data.unpersist()
  }

  test("k-means|| initialization") {

    case class VectorWithCompare(x: Vector) extends Ordered[VectorWithCompare] {
      @Override def compare(that: VectorWithCompare): Int = {
        if (this.x.toArray.foldLeft[Double](0.0)((acc, x) => acc + x * x) >
          that.x.toArray.foldLeft[Double](0.0)((acc, x) => acc + x * x)) -1
        else 1
      }
    }

    val points = Seq(
      Vectors.dense(1.0, 2.0, 6.0),
      Vectors.dense(1.0, 3.0, 0.0),
      Vectors.dense(1.0, 4.0, 6.0),
      Vectors.dense(1.0, 0.0, 1.0),
      Vectors.dense(1.0, 1.0, 1.0)
    )
    val rdd = sc.parallelize(points)

    // K-means|| initialization should place all clusters into distinct centers because
    // it will make at least five passes, and it will give non-zero probability to each
    // unselected point as long as it hasn't yet selected all of them

    var model = KMeans.train(rdd, k = 5, maxIterations = 1)

    assert(model.clusterCenters.sortBy(VectorWithCompare(_))
      .zip(points.sortBy(VectorWithCompare(_))).forall(x => x._1 ~== (x._2) absTol 1E-5))

    // Iterations of Lloyd's should not change the answer either
    model = KMeans.train(rdd, k = 5, maxIterations = 10)
    assert(model.clusterCenters.sortBy(VectorWithCompare(_))
      .zip(points.sortBy(VectorWithCompare(_))).forall(x => x._1 ~== (x._2) absTol 1E-5))

    // Neither should more runs
    model = KMeans.train(rdd, k = 5, maxIterations = 10, runs = 5)
    assert(model.clusterCenters.sortBy(VectorWithCompare(_))
      .zip(points.sortBy(VectorWithCompare(_))).forall(x => x._1 ~== (x._2) absTol 1E-5))
  }

  test("two clusters") {
    val points = Seq(
      Vectors.dense(0.0, 0.0),
      Vectors.dense(0.0, 0.1),
      Vectors.dense(0.1, 0.0),
      Vectors.dense(9.0, 0.0),
      Vectors.dense(9.0, 0.2),
      Vectors.dense(9.2, 0.0)
    )
    val rdd = sc.parallelize(points, 3)

    for (initMode <- Seq(RANDOM, K_MEANS_PARALLEL)) {
      // Two iterations are sufficient no matter where the initial centers are.
      val model = KMeans.train(rdd, k = 2, maxIterations = 2, runs = 1, initializerName = initMode)

      val predicts = model.predict(rdd).collect()

      assert(predicts(0) === predicts(1))
      assert(predicts(0) === predicts(2))
      assert(predicts(3) === predicts(4))
      assert(predicts(3) === predicts(5))
      assert(predicts(0) != predicts(3))
    }
  }
}

class KMeansClusterSuite extends FunSuite with LocalClusterSparkContext {

  test("task size should be small in both training and prediction") {
    val m = 4
    val n = 200000
    val points = sc.parallelize(0 until m, 2).mapPartitionsWithIndex { (idx, iter) =>
      val random = new Random(idx)
      iter.map(i => Vectors.dense(Array.fill(n)(random.nextDouble())))
    }.cache()
    for (initMode <- Seq(KMeans.RANDOM, KMeans.K_MEANS_PARALLEL)) {
      // If we serialize data directly in the task closure, the size of the serialized task would be
      // greater than 1MB and hence Spark would throw an error.
      val model = KMeans.train(points, 2, 2, 1, initMode)
      val predictions = model.predict(points).collect()
      val cost = model.computeCost(points)
    }
  }
}