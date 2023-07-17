package org.jgi.spark.localcluster.tools

import com.holdenkarau.spark.testing.SharedSparkContext
import org.apache.spark.sql.SparkSession
import org.apache.spark.{SparkConf, SparkContext}
import org.jgi.spark.localcluster.DNASeq
import org.jgi.spark.localcluster.tools.Seq2Parquet.APPNAME
import org.scalatest.{FlatSpec, Matchers, _}
import sext._

/**
  * Created by Lizhen Shi on 5/14/17.
  */
class KmerCountingSpec extends FlatSpec with Matchers  with SharedSparkContext
{
  override  def conf: SparkConf = super.conf.set("spark.ui.enabled", "true")

  private val master = "local[4]"
  private val appName = "KmerCountingSpec"

  "parse command line" should "be good" in {
    val cfg = KmerCounting.parse_command_line("-k 31 -i test -p *.seq -o tmp".split(" ")).get
    cfg.input should be("test")
  }
  "kmer counting" should "work on the test seq files" in {
    val cfg = KmerCounting.parse_command_line("-i data/small -k 31 -p sample.seq -o tmp/kmercounting_seq_test.txt --n_iteration 1".split(" ")).get
    print(s"called with arguments\n${cfg.valueTreeString}")
    KmerCounting.run(cfg, sc)
  }

  "kmer counting" should "work on the test seq files with canonical_kmer" in {
    val cfg = KmerCounting.parse_command_line("-i data/small -k 31 -C -p sample.seq -o tmp/kmercounting_seq_test_C.txt --n_iteration 1".split(" ")).get
    print(s"called with arguments\n${cfg.valueTreeString}")
    KmerCounting.run(cfg, sc)
  }

  "kmer counting" should "work on the test seq files with multiple N" in {
    val cfg = KmerCounting.parse_command_line("-i data/small -k 31 -p sample2.seq -o tmp/kmercounting_seq_sample2.txt --n_iteration 1".split(" ")).get
    print(s"called with arguments\n${cfg.valueTreeString}")
    KmerCounting.run(cfg, sc)
  }


  "kmer counting" should "work on the test base64 files" in {
    val cfg = KmerCounting.parse_command_line("-i data/small -k 31 -p sample.base64 --format base64 -o tmp/kmercounting_base64_test.txt --n_iteration 3".split(" ")).get
    print(s"called with arguments\n${cfg.valueTreeString}")
    KmerCounting.run(cfg, sc)
  }

  "kmer counting" should "work on the test base64 files with multiple N" in {
    val cfg = KmerCounting.parse_command_line("-i data/small -k 31 -p sample2.base64 --format base64 -o tmp/kmercounting_base64_sample2.txt --n_iteration 2".split(" ")).get
    print(s"called with arguments\n${cfg.valueTreeString}")
    KmerCounting.run(cfg, sc)
  }

  //*************************************** fake
  "kmer counting" should "work on fake seq file" in {
    val cfg = KmerCounting.parse_command_line("-i data/small -k 31 -p fake_sample.seq -o tmp/kmercounting_seq_fake.txt --n_iteration 1".split(" ")).get
    print(s"called with arguments\n${cfg.valueTreeString}")
    KmerCounting.run(cfg, sc)
  }

}
