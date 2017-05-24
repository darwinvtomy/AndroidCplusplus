package com.example.darwi.androidcplusplus;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

public class MainActivity extends AppCompatActivity {
   String TAG = "CRAP_CODE";
    // Used to load the 'native-lib' library on application startup.
/*    static {
        System.loadLibrary("native-lib");
    }*/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Example of a call to a native method
     //   TextView tv = (TextView) findViewById(R.id.sample_text);
     //   tv.setText(stringFromJNI());

        try {
            dotheStuff();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void dotheStuff() throws IOException {
      /*  String inputfilePath = Environment.getExternalStorageDirectory()
                .getPath() + "/" + "songwav.mp4";*/
        String outputFilePath = Environment.getExternalStorageDirectory()
                .getPath() + "/" + "songwavmp4.pcm";
        OutputStream outputStream = new FileOutputStream(outputFilePath);
        MediaCodec codec;
        AudioTrack audioTrack;

// extractor gets information about the stream
        MediaExtractor extractor = new MediaExtractor();
       // extractor.setDataSource("http://cds.t2z5e4w2.hwcdn.net/VALERIAN.mp4");
        extractor.setDataSource("http://html5demos.com/assets/dizzy.mp4");
       // MediaFormat format = extractor.getTrackFormat(1); //TODO if Content is AC3
        MediaFormat format = extractor.getTrackFormat(0);
        String mime = format.getString(MediaFormat.KEY_MIME);
        Log.e(TAG, "dotheStuff: MIME TYPE "+mime );
        // the actual decoder
        codec = MediaCodec.createDecoderByType(mime);
        codec.configure(format, null /* surface */, null /* crypto */, 0 /* flags */);
        codec.start();
        ByteBuffer[] codecInputBuffers = codec.getInputBuffers();
        ByteBuffer[] codecOutputBuffers = codec.getOutputBuffers();

        // get the sample rate to configure AudioTrack
        int sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE);


        // create our AudioTrack instance
        audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate,
                AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT,
                AudioTrack.getMinBufferSize(sampleRate,
                        AudioFormat.CHANNEL_OUT_STEREO,
                        AudioFormat.ENCODING_PCM_16BIT), AudioTrack.MODE_STREAM);

        // start playing, we will feed you later
        audioTrack.play();
        extractor.selectTrack(0);

        // start decoding
        final long kTimeOutUs = 10000;
        MediaCodec.BufferInfo BufInfo = new MediaCodec.BufferInfo();
        boolean sawInputEOS = false;
        boolean sawOutputEOS = false;

        int inputBufIndex;

        int counter=0;
        while (!sawOutputEOS) {


            counter++;
            if (!sawInputEOS) {
                inputBufIndex = codec.dequeueInputBuffer(kTimeOutUs);
                // Log.d(LOG_TAG, " bufIndexCheck " + bufIndexCheck);
                if (inputBufIndex >= 0) {
                    ByteBuffer dstBuf = codecInputBuffers[inputBufIndex];

                    int sampleSize = extractor
                            .readSampleData(dstBuf, 0 /* offset */);

                    long presentationTimeUs = 0;

                    if (sampleSize < 0) {

                        sawInputEOS = true;
                        sampleSize = 0;
                    } else {

                        presentationTimeUs = extractor.getSampleTime();
                    }
                    // can throw illegal state exception (???)

                    codec.queueInputBuffer(inputBufIndex, 0 /* offset */,
                            sampleSize, presentationTimeUs,
                            sawInputEOS ? MediaCodec.BUFFER_FLAG_END_OF_STREAM
                                    : 0);

                    if (!sawInputEOS) {
                        extractor.advance();
                    }
                } else {
                    Log.e("sohail", "inputBufIndex " + inputBufIndex);
                }
            }

            int res = codec.dequeueOutputBuffer(BufInfo, kTimeOutUs);

            if (res >= 0) {
                Log.i("sohail","decoding: deqOutputBuffer >=0, counter="+counter);
                // Log.d(LOG_TAG, "got frame, size " + info.size + "/" +
                // info.presentationTimeUs);
                if (BufInfo.size > 0) {
                    // noOutputCounter = 0;
                }

                int outputBufIndex = res;
                ByteBuffer buf = codecOutputBuffers[outputBufIndex];

                final byte[] chunk = new byte[BufInfo.size];
                buf.get(chunk);
                buf.clear();

                if (chunk.length > 0) {
                    // play
                    audioTrack.write(chunk, 0, chunk.length);
                    // write to file
                    outputStream.write(chunk);

                }
                codec.releaseOutputBuffer(outputBufIndex, false /* render */);
                if ((BufInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    Log.i("sohail", "saw output EOS.");
                    sawOutputEOS = true;
                }
            } else if (res == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                codecOutputBuffers = codec.getOutputBuffers();

                Log.i("sohail", "output buffers have changed.");
            } else if (res == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                MediaFormat oformat = codec.getOutputFormat();

                Log.i("sohail", "output format has changed to " + oformat);
            } else {
                Log.i("sohail", "dequeueOutputBuffer returned " + res);
            }
        }

        Log.d("sohail", "stopping...");

        // ////////closing
        if (audioTrack != null) {
            audioTrack.flush();
            audioTrack.release();
            audioTrack = null;
        }

        outputStream.flush();
        outputStream.close();

        codec.stop();
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
  //  public native String stringFromJNI();
}
