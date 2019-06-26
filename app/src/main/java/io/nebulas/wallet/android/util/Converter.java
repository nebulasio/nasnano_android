package io.nebulas.wallet.android.util;

import android.databinding.BindingAdapter;
import android.databinding.BindingConversion;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.widget.ImageView;
import android.widget.TextView;

import io.nebulas.wallet.android.common.Constants;
import io.nebulas.wallet.android.image.ImageUtil;

import java.io.File;
import java.text.DecimalFormat;
import java.util.Date;

/**
 * Created by Heinoc on 2018/2/28.
 */
public class Converter {

    /**
     * Data binding Date converter
     */
    @BindingConversion
    public static String convertDate(Date date) {
        if (null == date)
            return "";

        return Formatter.INSTANCE.timeFormat("yyyyMMMdkkmmss", date, false);
    }

    @BindingAdapter({"app:dateYYYYMMddHHmm"})
    public static void dateYYYYMMddHHmm(TextView textView, long date) {
        if (date == 0)
            return;

        textView.setText(Formatter.INSTANCE.timeFormat("YYYYMMdd HH:mm", date, false));

    }

    @BindingAdapter({"app:dateYYYYMMdd"})
    public static void dateYYYYMMdd(TextView textView, long date) {
        if (date == 0)
            return;

        textView.setText(Formatter.INSTANCE.timeFormat("yyyyMMMd", date, false));

    }

    @BindingAdapter({"app:dateMMddTime"})
    public static void dateMMddTime(TextView textView, long date) {
        if (date == 0)
            return;

        textView.setText(Formatter.INSTANCE.timeFormat("MMMdkkmm", date, false));

    }

    @BindingAdapter({"app:dateHHmmss"})
    public static void dateHHmmss(TextView textView, long date) {
        if (date == 0)
            return;

        textView.setText(Formatter.INSTANCE.timeFormat("kkmmss", date, true));

    }

    @BindingAdapter({"app:loadTokenBalance"})
    public static void loadTokenBalance(TextView textView, String data) {
        if (null == data)
            return;

        DecimalFormat format = new DecimalFormat(Constants.TOKEN_FORMAT);
        textView.setText(format.format(data));
    }

    @BindingAdapter({"app:loadCurrencyBalance"})
    public static void loadCurrencyBalance(TextView textView, String data) {
        if (null == data)
            return;

        DecimalFormat format = new DecimalFormat(Constants.TOKEN_FORMAT);
        textView.setText(format.format(data));
    }

    @BindingAdapter({"app:loadTimeStamp"})
    public static void loadTimeStamp(TextView textView, Long timestamp) {
        if (null == timestamp)
            return;
        textView.setText(convertDate(new Date(timestamp)));
    }

    @BindingAdapter({"app:imageUrl"})
    public static void loadImage(ImageView imageView, String url) {
        if (null == url)
            return;

        ImageUtil.INSTANCE.load(imageView.getContext(), imageView, url);
    }

    @BindingAdapter({"app:wei"})
    public static void weiToNas(TextView textView, String wei) {
        if (wei == null || wei.isEmpty()) {
            wei = "0";
        }
        wei = wei.trim().replace(",", "");
        String nas = Formatter.INSTANCE.nasBalanceFormat(wei, Constants.TOKEN_SCALE);
        textView.setText(nas);
    }

    @BindingAdapter("android:src")
    public static void setSrc(ImageView view, Bitmap bitmap) {
        view.setImageBitmap(bitmap);
    }

    @BindingAdapter("android:src")
    public static void setSrc(ImageView view, int resId) {
        view.setImageResource(resId);
    }

    @BindingAdapter({"app:addressBlockies"})
    public static void loadBlockies(ImageView imageView, String address) {
        if (address == null || address.isEmpty()) {
            return;
        }

        imageView.setImageBitmap(Blockies.INSTANCE.createIcon(address));
    }

    @BindingAdapter({"app:addressCircleBlockies"})
    public static void loadCircleBlockies(ImageView imageView, String address) {
        if (address == null || address.isEmpty()) {
            return;
        }

        imageView.setImageBitmap(Blockies.INSTANCE.createIcon(address, 16, true));
    }

    @BindingAdapter({"app:loadTXQRCode"})
    public static void loadTXQRCode(final ImageView imageView, String txUrl) {
        if (txUrl == null || txUrl.isEmpty()) {
            return;
        }

        String tempFileName = txUrl.replace("https://", "").replace("http://", "").replace("/", "_");
        final String qrCodeFilePath = FileTools.Companion.getDiskCacheDir(imageView.getContext(), Constants.TX_QRCODE_CACHE_DIR).getAbsolutePath() +
                File.separator + "qr_" + tempFileName + ".jpg";

        new AsyncTask<String, Long, String>() {
            @Override
            protected String doInBackground(String... strings) {
                boolean resultFlag = QRCodeUtil.INSTANCE.createQRImage(strings[0], 800, 800, null, qrCodeFilePath, false);

                return resultFlag ? qrCodeFilePath : null;
            }

            @Override
            protected void onPostExecute(String s) {
                super.onPostExecute(s);

                if (null != s && !s.isEmpty()) {
                    ImageUtil.INSTANCE.load(imageView.getContext(), imageView, qrCodeFilePath);
                }

            }
        }.execute(txUrl);


    }


}
