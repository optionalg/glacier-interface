package com.matoski.glacier.util;

import java.io.File;
import java.util.concurrent.Callable;

import com.matoski.glacier.pojo.Piece;

/**
 * A threaded dummy, used for completion.
 * 
 * @author Ilija Matoski (ilijamt@gmail.com)
 * 
 * @param <T>
 *          The class we use for the thread usually {@link Piece}
 */
public abstract class ThreadAmazonGlacierDummy<T> implements Callable<T> {

  protected T piece;
  protected int pieces;
  protected File file;

  /**
   * Constructor.
   * 
   * @param pieces
   *          How many pieces we have
   * @param file
   *          The filename to process
   * @param piece
   *          The piece we process
   * 
   */
  public ThreadAmazonGlacierDummy(int pieces, File file, T piece) {
    this.piece = piece;
    this.file = file;
    this.pieces = pieces;
  }

  /**
   * Override call that is used to process the dummy.
   */
  public abstract void process();

  @Override
  public T call() throws Exception {
    process();
    return piece;
  }

}
