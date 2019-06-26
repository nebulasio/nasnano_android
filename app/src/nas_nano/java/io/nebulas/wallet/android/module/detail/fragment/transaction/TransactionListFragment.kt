package io.nebulas.wallet.android.module.detail.fragment.transaction

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.nebulas.wallet.android.R

/**
 * Token详情页面的交易列表
 * Created by young on 2018/6/26.
 */
class TransactionListFragment : Fragment() {

    companion object {
        private const val PARAM_TOKEN_ID = "tokenId"
        fun newInstance(tokenId: String): TransactionListFragment {
            return TransactionListFragment().apply {
                arguments = Bundle().apply {
                    putString(PARAM_TOKEN_ID, tokenId)
                }
            }
        }
    }

    /**
     * 是否执行过了初始化数据加载，当此Fragment第一次在ViewPager中可见时，需要执行初始化数据方法，之后将此字段置为true
     */
    private var hasInitLoading = false

    private lateinit var dataCenter: TransactionListDataCenter
    private lateinit var controller: TransactionListController
    private lateinit var binder: TransactionListViewBinder
    private var tokenId: String = ""
    private lateinit var rootView: View

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_transaction_list_for_token_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        rootView = view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        tokenId = arguments?.getString(PARAM_TOKEN_ID, "") ?: ""
        dataCenter = TransactionListDataCenter(tokenId)
        controller = TransactionListController(this, requireActivity(), dataCenter)
        binder = TransactionListViewBinder(requireActivity())
        binder.bind(rootView, controller, dataCenter)
    }

    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)
        if (isVisibleToUser) {  //可见状态（在ViewPager中的focused item）
            if (!hasInitLoading) {  //没有执行过初始化数据加载
                hasInitLoading = true
                controller.refresh()  //执行初始化数据加载
            }
        }
    }

}