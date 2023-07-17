package org.jgi.spark.localcluster

/**
  * Created by Lizhen Shi on 5/13/17.
  */

import org.scalatest._

class KmerSpec extends FlatSpec with Matchers {

  "Kmer" should "be equal when the bases are same" in {
    DNASeq.from_bases("ATC") shouldEqual DNASeq.from_bases("ATC")
    DNASeq.from_bases("ATG") should not equal DNASeq.from_bases("ATC")
    DNASeq.from_bases("ATC").hashCode shouldEqual DNASeq.from_bases("ATC").hashCode
    DNASeq.from_bases("ATG").hashCode should not equal DNASeq.from_bases("ATC").hashCode

  }
  it should "think longer kmer is bigger" in {
    DNASeq.from_bases("ATCT") should be < DNASeq.from_bases("ATCTA")
  }

  "Kmer generator" should "generate 4 kmers since ATC is duplicate" in {
    val seq = "ATCGATC"
    val k = 3
    val kmers = Kmer.generate_kmer(seq, k)
    kmers.length shouldEqual 2
  }

  "Kmer generator" should "work as expected" in {
    val k =31
    val kmers1= Set("AGGGCAACAGAAGATGATAACAGAAAGGCAT","GGGCAACAGAAGATGATAACAGAAAGGCATT","GGCAACAGAAGATGATAACAGAAAGGCATTT","GCAACAGAAGATGATAACAGAAAGGCATTTT","CAACAGAAGATGATAACAGAAAGGCATTTTC","AACAGAAGATGATAACAGAAAGGCATTTTCC","ACAGAAGATGATAACAGAAAGGCATTTTCCA","CAGAAGATGATAACAGAAAGGCATTTTCCAT","AGAAGATGATAACAGAAAGGCATTTTCCATG","GAAGATGATAACAGAAAGGCATTTTCCATGC","AAGATGATAACAGAAAGGCATTTTCCATGCC","AGATGATAACAGAAAGGCATTTTCCATGCCT","GATGATAACAGAAAGGCATTTTCCATGCCTA","ATGATAACAGAAAGGCATTTTCCATGCCTAT","TGATAACAGAAAGGCATTTTCCATGCCTATA","GATAACAGAAAGGCATTTTCCATGCCTATAT","ATAACAGAAAGGCATTTTCCATGCCTATATT","TAACAGAAAGGCATTTTCCATGCCTATATTC","AACAGAAAGGCATTTTCCATGCCTATATTCG","ACAGAAAGGCATTTTCCATGCCTATATTCGG","CAGAAAGGCATTTTCCATGCCTATATTCGGT","AGAAAGGCATTTTCCATGCCTATATTCGGTC","GAAAGGCATTTTCCATGCCTATATTCGGTCG","AAAGGCATTTTCCATGCCTATATTCGGTCGT","AAGGCATTTTCCATGCCTATATTCGGTCGTT","AGGCATTTTCCATGCCTATATTCGGTCGTTT","GGCATTTTCCATGCCTATATTCGGTCGTTTT","GCATTTTCCATGCCTATATTCGGTCGTTTTA","CATTTTCCATGCCTATATTCGGTCGTTTTAC","ATTTTCCATGCCTATATTCGGTCGTTTTACC","TTTTCCATGCCTATATTCGGTCGTTTTACCC","TTTCCATGCCTATATTCGGTCGTTTTACCCA","TTCCATGCCTATATTCGGTCGTTTTACCCAG","TCCATGCCTATATTCGGTCGTTTTACCCAGA","CCATGCCTATATTCGGTCGTTTTACCCAGAG","CATGCCTATATTCGGTCGTTTTACCCAGAGA","ATGCCTATATTCGGTCGTTTTACCCAGAGAG","TGCCTATATTCGGTCGTTTTACCCAGAGAGC","GCCTATATTCGGTCGTTTTACCCAGAGAGCC","CCTATATTCGGTCGTTTTACCCAGAGAGCCC","CTATATTCGGTCGTTTTACCCAGAGAGCCCA","TATATTCGGTCGTTTTACCCAGAGAGCCCAG","ATATTCGGTCGTTTTACCCAGAGAGCCCAGC","TATTCGGTCGTTTTACCCAGAGAGCCCAGCA","ATTCGGTCGTTTTACCCAGAGAGCCCAGCAG","TTCGGTCGTTTTACCCAGAGAGCCCAGCAGA","TCGGTCGTTTTACCCAGAGAGCCCAGCAGAC","CGGTCGTTTTACCCAGAGAGCCCAGCAGACA","GGTCGTTTTACCCAGAGAGCCCAGCAGACAC","GTCGTTTTACCCAGAGAGCCCAGCAGACACT","TCGTTTTACCCAGAGAGCCCAGCAGACACTG","CGTTTTACCCAGAGAGCCCAGCAGACACTGA","GTTTTACCCAGAGAGCCCAGCAGACACTGAT","TTTTACCCAGAGAGCCCAGCAGACACTGATG","TTTACCCAGAGAGCCCAGCAGACACTGATGC","TTACCCAGAGAGCCCAGCAGACACTGATGCT","TACCCAGAGAGCCCAGCAGACACTGATGCTG","ACCCAGAGAGCCCAGCAGACACTGATGCTGG","CCCAGAGAGCCCAGCAGACACTGATGCTGGC","CCAGAGAGCCCAGCAGACACTGATGCTGGCC","CAGAGAGCCCAGCAGACACTGATGCTGGCCC","AGAGAGCCCAGCAGACACTGATGCTGGCCCA","GAGAGCCCAGCAGACACTGATGCTGGCCCAG","AGAGCCCAGCAGACACTGATGCTGGCCCAGC","GAGCCCAGCAGACACTGATGCTGGCCCAGCG","AGCCCAGCAGACACTGATGCTGGCCCAGCGG","GCCCAGCAGACACTGATGCTGGCCCAGCGGA","CCCAGCAGACACTGATGCTGGCCCAGCGGAT","CCAGCAGACACTGATGCTGGCCCAGCGGATC","CAGCAGACACTGATGCTGGCCCAGCGGATCG","AGCAGACACTGATGCTGGCCCAGCGGATCGC","GCGATCCGCTGGGCCAGCATCAGTGTCTGCT","CGATCCGCTGGGCCAGCATCAGTGTCTGCTG","GATCCGCTGGGCCAGCATCAGTGTCTGCTGG","ATCCGCTGGGCCAGCATCAGTGTCTGCTGGG","TCCGCTGGGCCAGCATCAGTGTCTGCTGGGC","CCGCTGGGCCAGCATCAGTGTCTGCTGGGCT","CGCTGGGCCAGCATCAGTGTCTGCTGGGCTC","GCTGGGCCAGCATCAGTGTCTGCTGGGCTCT","CTGGGCCAGCATCAGTGTCTGCTGGGCTCTC","TGGGCCAGCATCAGTGTCTGCTGGGCTCTCT","GGGCCAGCATCAGTGTCTGCTGGGCTCTCTG","GGCCAGCATCAGTGTCTGCTGGGCTCTCTGG","GCCAGCATCAGTGTCTGCTGGGCTCTCTGGG","CCAGCATCAGTGTCTGCTGGGCTCTCTGGGT","CAGCATCAGTGTCTGCTGGGCTCTCTGGGTA","AGCATCAGTGTCTGCTGGGCTCTCTGGGTAA","GCATCAGTGTCTGCTGGGCTCTCTGGGTAAA","CATCAGTGTCTGCTGGGCTCTCTGGGTAAAA","ATCAGTGTCTGCTGGGCTCTCTGGGTAAAAC","TCAGTGTCTGCTGGGCTCTCTGGGTAAAACG","CAGTGTCTGCTGGGCTCTCTGGGTAAAACGA","AGTGTCTGCTGGGCTCTCTGGGTAAAACGAC","GTGTCTGCTGGGCTCTCTGGGTAAAACGACC","TGTCTGCTGGGCTCTCTGGGTAAAACGACCG","GTCTGCTGGGCTCTCTGGGTAAAACGACCGA","TCTGCTGGGCTCTCTGGGTAAAACGACCGAA","CTGCTGGGCTCTCTGGGTAAAACGACCGAAT","TGCTGGGCTCTCTGGGTAAAACGACCGAATA","GCTGGGCTCTCTGGGTAAAACGACCGAATAT","CTGGGCTCTCTGGGTAAAACGACCGAATATA","TGGGCTCTCTGGGTAAAACGACCGAATATAG","GGGCTCTCTGGGTAAAACGACCGAATATAGG","GGCTCTCTGGGTAAAACGACCGAATATAGGC","GCTCTCTGGGTAAAACGACCGAATATAGGCA","CTCTCTGGGTAAAACGACCGAATATAGGCAT","TCTCTGGGTAAAACGACCGAATATAGGCATG","CTCTGGGTAAAACGACCGAATATAGGCATGG","TCTGGGTAAAACGACCGAATATAGGCATGGA","CTGGGTAAAACGACCGAATATAGGCATGGAA","TGGGTAAAACGACCGAATATAGGCATGGAAA","GGGTAAAACGACCGAATATAGGCATGGAAAA","GGTAAAACGACCGAATATAGGCATGGAAAAT","GTAAAACGACCGAATATAGGCATGGAAAATG","TAAAACGACCGAATATAGGCATGGAAAATGC","AAAACGACCGAATATAGGCATGGAAAATGCC","AAACGACCGAATATAGGCATGGAAAATGCCT","AACGACCGAATATAGGCATGGAAAATGCCTT","ACGACCGAATATAGGCATGGAAAATGCCTTT","CGACCGAATATAGGCATGGAAAATGCCTTTC","GACCGAATATAGGCATGGAAAATGCCTTTCT","ACCGAATATAGGCATGGAAAATGCCTTTCTG","CCGAATATAGGCATGGAAAATGCCTTTCTGT","CGAATATAGGCATGGAAAATGCCTTTCTGTT","GAATATAGGCATGGAAAATGCCTTTCTGTTA","AATATAGGCATGGAAAATGCCTTTCTGTTAT","ATATAGGCATGGAAAATGCCTTTCTGTTATC","TATAGGCATGGAAAATGCCTTTCTGTTATCA","ATAGGCATGGAAAATGCCTTTCTGTTATCAT","TAGGCATGGAAAATGCCTTTCTGTTATCATC","AGGCATGGAAAATGCCTTTCTGTTATCATCT","GGCATGGAAAATGCCTTTCTGTTATCATCTT","GCATGGAAAATGCCTTTCTGTTATCATCTTC","CATGGAAAATGCCTTTCTGTTATCATCTTCT","ATGGAAAATGCCTTTCTGTTATCATCTTCTG","TGGAAAATGCCTTTCTGTTATCATCTTCTGT","GGAAAATGCCTTTCTGTTATCATCTTCTGTT","GAAAATGCCTTTCTGTTATCATCTTCTGTTG","AAAATGCCTTTCTGTTATCATCTTCTGTTGC","AAATGCCTTTCTGTTATCATCTTCTGTTGCC","AATGCCTTTCTGTTATCATCTTCTGTTGCCC","ATGCCTTTCTGTTATCATCTTCTGTTGCCCT","TCTGTCCCAGCTTATGGGATTCCAGCACGCT","CTGTCCCAGCTTATGGGATTCCAGCACGCTG","TGTCCCAGCTTATGGGATTCCAGCACGCTGT","GTCCCAGCTTATGGGATTCCAGCACGCTGTT","TCCCAGCTTATGGGATTCCAGCACGCTGTTT","CCCAGCTTATGGGATTCCAGCACGCTGTTTT","CCAGCTTATGGGATTCCAGCACGCTGTTTTC","CAGCTTATGGGATTCCAGCACGCTGTTTTCC","AGCTTATGGGATTCCAGCACGCTGTTTTCCA","GCTTATGGGATTCCAGCACGCTGTTTTCCAG","CTTATGGGATTCCAGCACGCTGTTTTCCAGC","TTATGGGATTCCAGCACGCTGTTTTCCAGCG","TATGGGATTCCAGCACGCTGTTTTCCAGCGT","ATGGGATTCCAGCACGCTGTTTTCCAGCGTT","TGGGATTCCAGCACGCTGTTTTCCAGCGTTT","GGGATTCCAGCACGCTGTTTTCCAGCGTTTT","GGATTCCAGCACGCTGTTTTCCAGCGTTTTC","GATTCCAGCACGCTGTTTTCCAGCGTTTTCT","ATTCCAGCACGCTGTTTTCCAGCGTTTTCTT","TTCCAGCACGCTGTTTTCCAGCGTTTTCTTT","TCCAGCACGCTGTTTTCCAGCGTTTTCTTTG","CCAGCACGCTGTTTTCCAGCGTTTTCTTTGC","CAGCACGCTGTTTTCCAGCGTTTTCTTTGCC","AGCACGCTGTTTTCCAGCGTTTTCTTTGCCC","GCACGCTGTTTTCCAGCGTTTTCTTTGCCCT","CACGCTGTTTTCCAGCGTTTTCTTTGCCCTG","ACGCTGTTTTCCAGCGTTTTCTTTGCCCTGG","CGCTGTTTTCCAGCGTTTTCTTTGCCCTGGG","GCTGTTTTCCAGCGTTTTCTTTGCCCTGGGG","CTGTTTTCCAGCGTTTTCTTTGCCCTGGGGC","TGTTTTCCAGCGTTTTCTTTGCCCTGGGGCT","GTTTTCCAGCGTTTTCTTTGCCCTGGGGCTC","TTTTCCAGCGTTTTCTTTGCCCTGGGGCTCA","TTTCCAGCGTTTTCTTTGCCCTGGGGCTCAG","TTCCAGCGTTTTCTTTGCCCTGGGGCTCAGC","TCCAGCGTTTTCTTTGCCCTGGGGCTCAGCT","CCAGCGTTTTCTTTGCCCTGGGGCTCAGCTC","CAGCGTTTTCTTTGCCCTGGGGCTCAGCTCG","AGCGTTTTCTTTGCCCTGGGGCTCAGCTCGA","GCGTTTTCTTTGCCCTGGGGCTCAGCTCGAT","CGTTTTCTTTGCCCTGGGGCTCAGCTCGATC","GTTTTCTTTGCCCTGGGGCTCAGCTCGATCC","TTTTCTTTGCCCTGGGGCTCAGCTCGATCCG","TTTCTTTGCCCTGGGGCTCAGCTCGATCCGG","TTCTTTGCCCTGGGGCTCAGCTCGATCCGGT","TCTTTGCCCTGGGGCTCAGCTCGATCCGGTT","CTTTGCCCTGGGGCTCAGCTCGATCCGGTTT","TTTGCCCTGGGGCTCAGCTCGATCCGGTTTC","TTGCCCTGGGGCTCAGCTCGATCCGGTTTCC","TGCCCTGGGGCTCAGCTCGATCCGGTTTCCT","GCCCTGGGGCTCAGCTCGATCCGGTTTCCTC","CCCTGGGGCTCAGCTCGATCCGGTTTCCTCT","CCTGGGGCTCAGCTCGATCCGGTTTCCTCTG","CTGGGGCTCAGCTCGATCCGGTTTCCTCTGG","TGGGGCTCAGCTCGATCCGGTTTCCTCTGGC","GGGGCTCAGCTCGATCCGGTTTCCTCTGGCC","GGGCTCAGCTCGATCCGGTTTCCTCTGGCCT","GGCTCAGCTCGATCCGGTTTCCTCTGGCCTC","GCTCAGCTCGATCCGGTTTCCTCTGGCCTCT","CTCAGCTCGATCCGGTTTCCTCTGGCCTCTT","TCAGCTCGATCCGGTTTCCTCTGGCCTCTTC","CAGCTCGATCCGGTTTCCTCTGGCCTCTTCC","AGCTCGATCCGGTTTCCTCTGGCCTCTTCCG","GCTCGATCCGGTTTCCTCTGGCCTCTTCCGG","CTCGATCCGGTTTCCTCTGGCCTCTTCCGGA","TCGATCCGGTTTCCTCTGGCCTCTTCCGGAT","CGATCCGGTTTCCTCTGGCCTCTTCCGGATC","GATCCGGTTTCCTCTGGCCTCTTCCGGATCA","ATCCGGTTTCCTCTGGCCTCTTCCGGATCAC","TCCGGTTTCCTCTGGCCTCTTCCGGATCACC","CCGGTTTCCTCTGGCCTCTTCCGGATCACCG","CGGTGATCCGGAAGAGGCCAGAGGAAACCGG","GGTGATCCGGAAGAGGCCAGAGGAAACCGGA","GTGATCCGGAAGAGGCCAGAGGAAACCGGAT","TGATCCGGAAGAGGCCAGAGGAAACCGGATC","GATCCGGAAGAGGCCAGAGGAAACCGGATCG","ATCCGGAAGAGGCCAGAGGAAACCGGATCGA","TCCGGAAGAGGCCAGAGGAAACCGGATCGAG","CCGGAAGAGGCCAGAGGAAACCGGATCGAGC","CGGAAGAGGCCAGAGGAAACCGGATCGAGCT","GGAAGAGGCCAGAGGAAACCGGATCGAGCTG","GAAGAGGCCAGAGGAAACCGGATCGAGCTGA","AAGAGGCCAGAGGAAACCGGATCGAGCTGAG","AGAGGCCAGAGGAAACCGGATCGAGCTGAGC","GAGGCCAGAGGAAACCGGATCGAGCTGAGCC","AGGCCAGAGGAAACCGGATCGAGCTGAGCCC","GGCCAGAGGAAACCGGATCGAGCTGAGCCCC","GCCAGAGGAAACCGGATCGAGCTGAGCCCCA","CCAGAGGAAACCGGATCGAGCTGAGCCCCAG","CAGAGGAAACCGGATCGAGCTGAGCCCCAGG","AGAGGAAACCGGATCGAGCTGAGCCCCAGGG","GAGGAAACCGGATCGAGCTGAGCCCCAGGGC","AGGAAACCGGATCGAGCTGAGCCCCAGGGCA","GGAAACCGGATCGAGCTGAGCCCCAGGGCAA","GAAACCGGATCGAGCTGAGCCCCAGGGCAAA","AAACCGGATCGAGCTGAGCCCCAGGGCAAAG","AACCGGATCGAGCTGAGCCCCAGGGCAAAGA","ACCGGATCGAGCTGAGCCCCAGGGCAAAGAA","CCGGATCGAGCTGAGCCCCAGGGCAAAGAAA","CGGATCGAGCTGAGCCCCAGGGCAAAGAAAA","GGATCGAGCTGAGCCCCAGGGCAAAGAAAAC","GATCGAGCTGAGCCCCAGGGCAAAGAAAACG","ATCGAGCTGAGCCCCAGGGCAAAGAAAACGC","TCGAGCTGAGCCCCAGGGCAAAGAAAACGCT","CGAGCTGAGCCCCAGGGCAAAGAAAACGCTG","GAGCTGAGCCCCAGGGCAAAGAAAACGCTGG","AGCTGAGCCCCAGGGCAAAGAAAACGCTGGA","GCTGAGCCCCAGGGCAAAGAAAACGCTGGAA","CTGAGCCCCAGGGCAAAGAAAACGCTGGAAA","TGAGCCCCAGGGCAAAGAAAACGCTGGAAAA","GAGCCCCAGGGCAAAGAAAACGCTGGAAAAC","AGCCCCAGGGCAAAGAAAACGCTGGAAAACA","GCCCCAGGGCAAAGAAAACGCTGGAAAACAG","CCCCAGGGCAAAGAAAACGCTGGAAAACAGC","CCCAGGGCAAAGAAAACGCTGGAAAACAGCG","CCAGGGCAAAGAAAACGCTGGAAAACAGCGT","CAGGGCAAAGAAAACGCTGGAAAACAGCGTG","AGGGCAAAGAAAACGCTGGAAAACAGCGTGC","GGGCAAAGAAAACGCTGGAAAACAGCGTGCT","GGCAAAGAAAACGCTGGAAAACAGCGTGCTG","GCAAAGAAAACGCTGGAAAACAGCGTGCTGG","CAAAGAAAACGCTGGAAAACAGCGTGCTGGA","AAAGAAAACGCTGGAAAACAGCGTGCTGGAA","AAGAAAACGCTGGAAAACAGCGTGCTGGAAT","AGAAAACGCTGGAAAACAGCGTGCTGGAATC","GAAAACGCTGGAAAACAGCGTGCTGGAATCC","AAAACGCTGGAAAACAGCGTGCTGGAATCCC","AAACGCTGGAAAACAGCGTGCTGGAATCCCA","AACGCTGGAAAACAGCGTGCTGGAATCCCAT","ACGCTGGAAAACAGCGTGCTGGAATCCCATA","CGCTGGAAAACAGCGTGCTGGAATCCCATAA","GCTGGAAAACAGCGTGCTGGAATCCCATAAG","CTGGAAAACAGCGTGCTGGAATCCCATAAGC","TGGAAAACAGCGTGCTGGAATCCCATAAGCT","GGAAAACAGCGTGCTGGAATCCCATAAGCTG","GAAAACAGCGTGCTGGAATCCCATAAGCTGG","AAAACAGCGTGCTGGAATCCCATAAGCTGGG","AAACAGCGTGCTGGAATCCCATAAGCTGGGA","AACAGCGTGCTGGAATCCCATAAGCTGGGAC","ACAGCGTGCTGGAATCCCATAAGCTGGGACA","CAGCGTGCTGGAATCCCATAAGCTGGGACAG","AGCGTGCTGGAATCCCATAAGCTGGGACAGA")
    kmers1.foreach {
      s=>
        val dna = DNASeq.from_bases(s)
        dna.to_bases(k) should equal ( s)
    }
    val seq ="AGGGCAACAGAAGATGATAACAGAAAGGCATTTTCCATGCCTATATTCGGTCGTTTTACCCAGAGAGCCCAGCAGACACTGATGCTGGCCCAGCGGATCGCNTCTGTCCCAGCTTATGGGATTCCAGCACGCTGTTTTCCAGCGTTTTCTTTGCCCTGGGGCTCAGCTCGATCCGGTTTCCTCTGGCCTCTTCCGGATCACCG"
    val kmers2 = Kmer2.generate_kmer(seq, k).map(_.to_bases(k)).toSet
    println(s"length: ${kmers1.size} vs ${kmers2.size}")
    println("diff1",kmers1.diff(kmers2).size)

    println("diff2",kmers2.diff(kmers1).size)

    kmers1  should  equal (kmers2 )
  }

  "Kmer generator" should "work for for canonical_kmer form" in {
    val s1 = "GCAAAGAAAACGCTGGAAAACAGCGTGCTGG"
    val s2="CCAGCACGCTGTTTTCCAGCGTTTTCTTTGC"
    DNASeq.reverse_complement(s1) should equal (s2)
    DNASeq.reverse_complement(s2) should equal (s1)

    Kmer.generate_kmer(s1,31)(0).to_bases(31) should equal  (s2)
    Kmer.generate_kmer(s2,31)(0).to_bases(31) should equal  (s2)

  }

}


