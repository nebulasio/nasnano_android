package io.nebulas.wallet.android.util;

import android.databinding.BindingAdapter;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.airbnb.lottie.LottieAnimationView;
import io.nebulas.wallet.android.R;
import io.nebulas.wallet.android.app.WalletApplication;
import io.nebulas.wallet.android.image.ImageUtil;
import org.jetbrains.anko.DimensionsKt;

import java.util.List;

/**
 * Created by Heinoc on 2018/2/28.
 */
public class AConverter {

    @BindingAdapter({"app:loopLottieFile"})
    public static void loopLottieFile(final LottieAnimationView lottieAnimationView, String fileName) {
        if (null == fileName || fileName.isEmpty())
            return;

        lottieAnimationView.setAnimation(fileName);
        lottieAnimationView.setRepeatMode(Animation.RESTART);
        lottieAnimationView.setRepeatCount(-1);
        lottieAnimationView.playAnimation();

    }


    static int[] ME_WALLET_BGS = {R.drawable.me_wallet_bg_one,
            R.drawable.me_wallet_bg_two,
            R.drawable.me_wallet_bg_three,
            R.drawable.me_wallet_bg_four,
            R.drawable.me_wallet_bg_five};

    @BindingAdapter({"app:loadWalletBG"})
    public static void loadWalletBG(final TextView textView, long walletId) {
        textView.setBackgroundResource(ME_WALLET_BGS[(int) walletId % 5]);

    }

    @BindingAdapter({"app:loadTextColor"})
    public static void loadTextColor(final TextView textView, int status) {
        int color = R.color.color_8350F6;

        if (status == 1) {
            color = R.color.color_8F8F8F;
        } else if (status == 2) {
            color = R.color.color_038AFB;

        }
        textView.setTextColor(ContextCompat.getColor(WalletApplication.Companion.getINSTANCE(), color));
    }

    /**
     * 用于加载资产页面“隐藏资产”和“钱包”卡片底部多个token icon
     *
     * @param linearLayout
     * @param tokenIcons
     */
    @BindingAdapter({"app:loadTokenIcons"})
    public static void loadTokenIconsWithLayoutCache(final LinearLayout linearLayout, List<String> tokenIcons) {
        if (tokenIcons == null) {
            return;
        }
        int tokenCount = tokenIcons.size();
        int count = Math.min(tokenCount, 6);
        int existChildCount = linearLayout.getChildCount();
        int height = DimensionsKt.dip(linearLayout, 30);
        int width = height;
        for (int i = 0; i < count; i++) {
            String tokenLogo = tokenIcons.get(i);
            ImageView imageView;
            if (i < existChildCount) {
                View child = linearLayout.getChildAt(i);
                if (child instanceof ImageView) {
                    child.setVisibility(View.VISIBLE);
                    imageView = (ImageView) child;
                } else {
                    imageView = addLogoImageViewToParent(linearLayout, width, height, i);
                }
            } else {
                imageView = addLogoImageViewToParent(linearLayout, width, height, i);
            }
            if (i < 5)
                ImageUtil.INSTANCE.load(linearLayout.getContext(), imageView, tokenLogo);
            else
                imageView.setImageResource(R.drawable.me_wallet_token_more);
        }
        int newExistChildCount = linearLayout.getChildCount();
        for (int i = count; i < newExistChildCount; i++) {
            linearLayout.getChildAt(i).setVisibility(View.GONE);
        }
    }

    private static ImageView addLogoImageViewToParent(LinearLayout parent, int width, int height, int index) {
        ImageView iv = new ImageView(parent.getContext());
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(width, height, 1);
        layoutParams.setMarginStart(Formatter.INSTANCE.dip2px(parent.getContext(), 6f));
        parent.addView(iv, index, layoutParams);
        return iv;
    }

}
