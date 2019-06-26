package io.nebulas.wallet.android.base

import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.nebulas.wallet.android.app.WalletApplication
import io.nebulas.wallet.android.network.exception.ApiException
import io.nebulas.wallet.android.util.NetWorkUtil

/**
 * Created by Heinoc on 2018/1/31.
 */

abstract class BaseFragment : Fragment() {
    override fun onAttach(context: Context?) {
        super.onAttach(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return super.onCreateView(inflater, container, savedInstanceState)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
    }

    abstract fun initView()

    override fun onStart() {
        super.onStart()
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onStop() {
        super.onStop()
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onDetach() {
        super.onDetach()
    }

    public fun isHasNetWork() : Boolean{
        if(!NetWorkUtil.instance.isNetWorkConnected()){
            WalletApplication.INSTANCE.activity?.toastErrorMessage(ApiException.CONNECT_EXCEPTION)
            return false
        }
        return true
    }
}