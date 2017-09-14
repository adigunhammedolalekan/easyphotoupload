package com.beem24.projects.easyphotoupload.listeners;

import java.io.File;
import java.util.List;

/**
 * Created by root on 9/14/17.
 */

public interface IImageCompressTaskListener {

    public void onComplete(List<File> compressed);
    public void onError(Throwable error);
}
