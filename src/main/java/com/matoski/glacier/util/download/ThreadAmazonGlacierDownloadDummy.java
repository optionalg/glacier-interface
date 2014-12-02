package com.matoski.glacier.util.download;

import java.io.File;

import com.matoski.glacier.pojo.download.DownloadPiece;
import com.matoski.glacier.util.ThreadAmazonGlacierDummy;

/**
 * A threaded dummy, used for completion
 * 
 * @author Ilija Matoski (ilijamt@gmail.com)
 */
public class ThreadAmazonGlacierDownloadDummy extends ThreadAmazonGlacierDummy<DownloadPiece> {

  /**
   * Constructor.
   * 
   * @param pieces
   *          How many pieces
   * @param file
   *          What is the filename
   * @param piece
   *          The download piece data
   */
  public ThreadAmazonGlacierDownloadDummy(int pieces, File file, DownloadPiece piece) {
    super(pieces, file, piece);
  }

  @Override
  public void process() {
    System.out.println(String.format(AmazonGlacierDownloadUtil.FORMAT, piece.getPart() + 1, pieces,
        piece.getStatus(), file, "Already downloaded"));
  }

}
