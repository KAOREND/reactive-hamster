
hamster.scrolling = {};

hamster.scrolling.noOnScroll = false;
hamster.scrolling.resetScrollPosition = new Array();

hamster.scrolling.resetEndlessScroll = function(listid) {
    if (hamster.scrolling.resetScrollPosition[listid]) {
        hamster.scrolling.resetScrollPosition[listid]();
    }
}

hamster.scrolling.setEndlessScroll = function (url, listid, reverse) {
    var list = $('#' + listid);
    var oldScrollPosition = list[0].scrollTop;
    var onScrollHandler = function () {
        if (hamster.main.pageUpdateRunning) {
            return;
        }
        var list = $('#' + listid);
        hamster.main.log('scrolling');
        var isScrollingUp = oldScrollPosition > list[0].scrollTop;
        oldScrollPosition = list[0].scrollTop;
        var oldScrollHeight = list[0].scrollHeight;
        if (list && !hamster.scrolling.noOnScroll  && !hamster.main.pageUpdateRunning && !hamster.main.hasHashURLChanged()) {
            //only if this list visible
            var scrollRange = list[0].scrollHeight - list.outerHeight();
            var scrollPercentage = list[0].scrollTop / scrollRange;
            if ((reverse && scrollPercentage < 0.2 && isScrollingUp) || (!reverse && scrollPercentage > 0.8 && !isScrollingUp)) {
                hamster.scrolling.resetScrollPosition[listid] = function () {
                    updateLayout();
                    var list = $('#' + listid);
                    if ((isScrollingUp && reverse) || (!isScrollingUp && !reverse)) {
                        hamster.scrolling.noOnScroll  = true;
                        if (reverse) {
                            var newSrcollHeight = list[0].scrollHeight;
                            var restoredScrollPosition = oldScrollPosition + (newSrcollHeight - oldScrollHeight);
                            list.scrollTop(restoredScrollPosition);
                        } else {
                            list.scrollTop(oldScrollPosition);
                        }
                        //list.scrollTo(restoredScrollPosition);
                        hamster.scrolling.noOnScroll  = false;
                    }
                    hamster.scrolling.resetScrollPosition[listid] = null;
                }
                list.off('scroll', onScrollHandler);
                hamster.main.doSilentRequest(url, 'GET', null, false);
            }
        }
    }
    list.on('scroll', onScrollHandler);
}

hamster.scrolling.setEndlessPageScroll = function (url, listid, reverse) {
    var oldScrollPosition = $(window).scrollTop();
    var onScrollHandler = function () {
        if (hamster.main.pageUpdateRunning) {
            return;
        }
        var list = document.getElementById(listid);
        var isScrollingUp = oldScrollPosition > $(window).scrollTop();
        oldScrollPosition = $(window).scrollTop();
        var oldScrollHeight = document.body.scrollHeight;
        if (list && !hamster.scrolling.noOnScroll  && !hamster.main.pageUpdateRunning && !hamster.main.hasHashURLChanged()) {
            //only if this list visible
            var scrollRange = document.body.scrollHeight - $(window).outerHeight();
            var scrollPercentage = $(window).scrollTop() / scrollRange;
            if ((reverse && scrollPercentage < 0.2 && isScrollingUp) || (!reverse && scrollPercentage > 0.8 && !isScrollingUp)) {
                hamster.scrolling.resetScrollPosition[listid] = function () {
                    if (isScrollingUp) {
                        hamster.scrolling.noOnScroll  = true;
                        var restoredScrollPosition = oldScrollPosition + (document.body.scrollHeight - oldScrollHeight);
                        window.scrollTo(0, restoredScrollPosition);
                        hamster.scrolling.noOnScroll  = false;
                    } else {
                        $(window).scrollTop(oldScrollPosition);
                    }
                    hamster.scrolling.resetScrollPosition[listid] = null;
                }
                window.removeEventListener('scroll', onScrollHandler);
                hamster.main.doSilentRequest(url, 'GET', null, false);
            }
        }
    }
    window.addEventListener('scroll', onScrollHandler);
}