package org.personal.coupleapp.utils

import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager

abstract class InfiniteScrollListener : RecyclerView.OnScrollListener {

    // 새로운 데이터를 가져오기 전 스크롤 위치 아래에 있는 최소 항목 수
    private var visibleThreshold = 3

    // 현재 페이지
    private var currentPage = 0

    // 전체 아이템 갯수
    private var previousTotalItemCount = 0

    // 마지막 데이터가 로드되기를 기다리는지 여부
    private var loading = true

    // 처음 페이지
    private val startingPageIndex = 0
    private var layoutManager: RecyclerView.LayoutManager

    constructor(layoutManager: LinearLayoutManager) {
        this.layoutManager = layoutManager
    }

    constructor(layoutManager: GridLayoutManager) {
        this.layoutManager = layoutManager
        visibleThreshold *= layoutManager.spanCount
    }

    constructor(layoutManager: StaggeredGridLayoutManager) {
        this.layoutManager = layoutManager
        visibleThreshold *= layoutManager.spanCount
    }

    private fun getLastVisibleItem(lastVisibleItemPositions: IntArray): Int {
        var maxSize = 0
        for (i in lastVisibleItemPositions.indices) {
            if (i == 0) {
                maxSize = lastVisibleItemPositions[i]
            } else if (lastVisibleItemPositions[i] > maxSize) {
                maxSize = lastVisibleItemPositions[i]
            }
        }
        return maxSize
    }

    override fun onScrolled(view: RecyclerView, dx: Int, dy: Int) {
        var lastVisibleItemPosition = 0
        val totalItemCount = layoutManager.itemCount
        when (layoutManager) {

            // 각기 다른 생성자로 레이아웃 매니저를 지정하면 각각 맞게 캐스팅해준다
            // 레이아웃 매니저의 종류에 따라 마지막의 아이템 포지션을 얻는 변수 지정이 다르기 때문에 다음과 같이 나눠 변수 지정
            is StaggeredGridLayoutManager -> {
                val lastVisibleItemPositions = (layoutManager as StaggeredGridLayoutManager).findLastVisibleItemPositions(null)
                // get maximum element within the list
                lastVisibleItemPosition = getLastVisibleItem(lastVisibleItemPositions)
            }
            is GridLayoutManager -> {
                lastVisibleItemPosition = (layoutManager as GridLayoutManager).findLastVisibleItemPosition()
            }
            is LinearLayoutManager -> {
                lastVisibleItemPosition = (layoutManager as LinearLayoutManager).findLastVisibleItemPosition()
            }
        }

        // 이전 항목 수 보다 총 항목 수가 적고
        if (totalItemCount < previousTotalItemCount) {
            currentPage = startingPageIndex
            previousTotalItemCount = totalItemCount
            if (totalItemCount == 0) {
                loading = true
            }
        }

        // 로딩 중이고, 이전 전체 아이템 갯수 보다 전체 갯수가 많아지면 -> 데이터가 업데이트가 된 것이기 때문에 로딩 false
        if (loading) {
            if (totalItemCount > previousTotalItemCount) {
                loading = false
                previousTotalItemCount = totalItemCount
            }
        }

        // 아이템 하단 겟수가 4개 이하이면 데이터를 더 부를 수 있도록 onLoadMore 을 실행
        if (!loading) {
            if (lastVisibleItemPosition + visibleThreshold > totalItemCount) {
                currentPage++
                onLoadMore(currentPage, totalItemCount, view)
                loading = true
            }
        }
    }

    // 페이징 값을 모두 초기화하기 위해 사용하는 메소드
    fun resetState() {
        currentPage = startingPageIndex
        previousTotalItemCount = 0
        loading = true
    }

    // 액티비티에서 추상 메소드 구현하기
    abstract fun onLoadMore(page: Int, totalItemsCount: Int, view: RecyclerView?)
}