app.controller('ClusterQueryCtrl', function ($scope, $state, $stateParams, $http, $interval, $timeout, $modal, $Popup) {
    $scope.id = $stateParams.id;
    $scope.name = $stateParams.name;

    $scope.query = {key: null, data: null};
    $scope.info = {key : null, data: null};
    var lastPage = {};
    var queryParam = {
        query: "*",
        cursor: "0",
        client: 0
    };

    var model = {
        script: null,
        cluster: $scope.id
    };

    $scope.search = function (key, flush) {
        if (key == null || key == "") {
            $scope.query.key = null;
            $scope.query.data = null;
            $scope.query.hasMore = null;
            return;
        }
        $scope.query.key = key;
        if (queryParam.query != key || flush) {
            queryParam = {
                query: key,
                cursor: "0",
                client: 0
            }
        }
        var loading = $modal.open({
            templateUrl: 'tpl/app/modal/loading.html'
        });
        $timeout(function () {
            loading.close();
        }, 3000);
        $http.post('query/scan/' + $scope.id, queryParam).success(function (response) {
            loading.close();
            lastPage = queryParam;
            queryParam.cursor = response.cursor;
            queryParam.client = response.client;
            $scope.query.data = response.keys;
            $scope.query.hasMore = response.hasMore;
        });
    };

    $scope.dbSize = function () {
        model = {
            script: "return redis.call('dbsize'), 0",
            cluster: $scope.id
        };

        var loading = $modal.open({
            templateUrl: 'tpl/app/modal/loading.html'
        });
        $timeout(function () {
            loading.close();
        }, 3000);
        $http.post('query/lua', model).success(function (response) {
            loading.close();
            $scope.query.info = response.data;
        });
    };

    $scope.deletes = function (key) {
        if (key == null || key == "") {
            $scope.query.key = null;
            $scope.query.result = null;
            return;
        }
        $Popup.confirm('警告', '确定要删除{' + key + '}匹配到的所有key?').then(function (flag) {
            if (flag) {
                model = {
                    script: key,
                    cluster: $scope.id
                };
                var loading = $modal.open({
                    templateUrl: 'tpl/app/modal/loading.html'
                });
                $timeout(function () {
                    loading.close();
                }, 3000);
                $http.post('query/deletes', model).success(function (response) {
                    loading.close();
                    $scope.query.result = response.data;
                });
            }
        });
    };

    $scope.delete_all = function () {
        $Popup.confirm('警告', '确定要清空redis?').then(function (flag) {
            if (flag) {
                var loading = $modal.open({
                    templateUrl: 'tpl/app/modal/loading.html'
                });
                $timeout(function () {
                    loading.close();
                }, 3000);
                $http.get('query/flush/' + $scope.id).success(function (response) {
                    loading.close();
                    if (response.status) {
                        $state.go('app.dashboard');
                    }
                });
            }
        });
    };

    $scope.next = function () {
        $scope.search(queryParam.query, false);
    };

    $scope.get = function (key) {
        $http.get('query/get/' + $scope.id + "/" + key).success(function (response) {
            $scope.JSON = JSON.stringify(response.data);
            var modalInstance = $modal.open({
                templateUrl: 'tpl/app/modal/json.html',
                scope: $scope
            });
            modalInstance.opened.then(function () {
                $scope.closeModal = function () {
                    modalInstance.close();
                }
            });
        });
    };
    $scope.del = function (key) {
        $Popup.confirm('警告', '这将删除key [' + key + ']?').then(function (flag) {
            if (flag) {
                $http.post('query/delete/' + $scope.id + "/" + key, {}).success(function (response) {
                    $http.post('query/scan/' + $scope.id, lastPage).success(function (response) {
                        queryParam.cursor = response.cursor;
                        queryParam.client = response.client;
                        $scope.query.data = response.keys;
                        $scope.query.hasMore = response.hasMore;
                    });
                });
            }
        });
    }
});