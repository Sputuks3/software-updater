# Technical Details #



## Patch ##
  * Encryption: [AES-256](http://home.comcast.net/~jwatne/aes.html) <sup>1</sup>
  * Compression: [LZMA 2](http://tukaani.org/xz/java.html)
  * Binary Diff: [XDelta](http://xdelta.org/) <sup>2</sup>
  * Integrity: SHA-256

  1. [JCE](http://en.wikipedia.org/wiki/Java_Cryptography_Extension) be limited to AES-128 by "Strong Jurisdiction Policy" [ref1](http://docs.oracle.com/javase/1.4.2/docs/guide/security/jce/JCERefGuide.html#AppE) [ref2](http://www.oracle.com/technetwork/java/javase/downloads/jce-6-download-429243.html)
  1. Java port by [javaxdelta](http://sourceforge.net/projects/javaxdelta/)

## Catalog ##
  * Authentication & Encryption: RSA
  * Compression: GZip