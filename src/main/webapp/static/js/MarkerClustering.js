
(function (global) {
    'use strict';

    function MarkerClustering(options) {
        this._map         = null;
        this._markers     = [];
        this._clusters    = [];
        this._minSize     = 2;
        this._maxZoom     = 13;
        this._gridSize    = 100;
        this._icons       = [];
        this._indexGen    = null;
        this._styleFn     = null;
        this._clickZoom   = true;
        this._idle        = null;

        if (options) this._applyOptions(options);
    }

    MarkerClustering.prototype._applyOptions = function (opts) {
        if (opts.minClusterSize  !== undefined) this._minSize   = opts.minClusterSize;
        if (opts.maxZoom         !== undefined) this._maxZoom   = opts.maxZoom;
        if (opts.gridSize        !== undefined) this._gridSize  = opts.gridSize;
        if (opts.icons           !== undefined) this._icons     = opts.icons;
        if (opts.indexGenerator  !== undefined) this._indexGen  = opts.indexGenerator;
        if (opts.stylingFunction !== undefined) this._styleFn   = opts.stylingFunction;
        if (opts.disableClickZoom!== undefined) this._clickZoom = !opts.disableClickZoom;
        if (opts.markers         !== undefined) this._markers   = opts.markers.slice();
        if (opts.map             !== undefined) this.setMap(opts.map);
    };

    MarkerClustering.prototype.setMap = function (map) {
        var self = this;

        // 기존 리스너 해제
        if (this._idle) {
            naver.maps.Event.removeListener(this._idle);
            this._idle = null;
        }

        this._map = map;
        this._clearClusters();

        if (!map) return;

        this._idle = naver.maps.Event.addListener(map, 'idle', function () {
            self._redraw();
        });

        this._redraw();
    };

    MarkerClustering.prototype._redraw = function () {
        this._clearClusters();
        if (!this._map) return;

        var zoom = this._map.getZoom();

        // 최대 줌 초과 시 개별 마커 직접 표시
        if (zoom > this._maxZoom) {
            var map = this._map;
            this._markers.forEach(function (m) { m.setMap(map); });
            return;
        }

        var proj     = this._map.getProjection();
        var gridSize = this._gridSize;

        // 마커를 그리드 셀에 배치
        var cells = [];  // { bounds, markers[] }

        this._markers.forEach(function (marker) {
            var pos   = marker.getPosition();
            var point = proj.fromCoordToOffset(pos);
            var gx    = Math.floor(point.x / gridSize);
            var gy    = Math.floor(point.y / gridSize);
            var key   = gx + ':' + gy;

            var cell = null;
            for (var i = 0; i < cells.length; i++) {
                if (cells[i].key === key) { cell = cells[i]; break; }
            }
            if (!cell) {
                cell = { key: key, gx: gx, gy: gy, markers: [] };
                cells.push(cell);
            }
            cell.markers.push(marker);
        });

        var map   = this._map;
        var self  = this;

        cells.forEach(function (cell) {
            var count = cell.markers.length;

            if (count < self._minSize) {
                // 개별 표시
                cell.markers.forEach(function (m) { m.setMap(map); });
                return;
            }

            // 클러스터 마커 생성
            var cx = 0, cy = 0;
            cell.markers.forEach(function (m) {
                var p = m.getPosition();
                cx += p.lat(); cy += p.lng();
            });
            var center = new naver.maps.LatLng(cx / count, cy / count);

            var index  = self._getIndex(count);
            var icon   = self._getIcon(index);

            var cm = new naver.maps.Marker({
                position: center,
                map: map,
                icon: icon,
                zIndex: 50
            });

            if (self._styleFn) self._styleFn(cm, count);

            if (self._clickZoom) {
                (function(markers) {
                    naver.maps.Event.addListener(cm, 'click', function () {
                        var bounds = new naver.maps.LatLngBounds();
                        markers.forEach(function(m) { bounds.extend(m.getPosition()); });
                        map.fitBounds(bounds);
                        // 너무 가까운 마커면 과도하게 확대되므로 최대 줌 16 제한
                        setTimeout(function() {
                            if (map.getZoom() > 16) map.setZoom(16);
                        }, 400);
                    });
                }(cell.markers.slice()));
            }

            self._clusters.push(cm);
        });
    };

    MarkerClustering.prototype._clearClusters = function () {
        this._clusters.forEach(function (cm) { cm.setMap(null); });
        this._clusters = [];
        this._markers.forEach(function (m) { if (m.getMap()) m.setMap(null); });
    };

    MarkerClustering.prototype._getIndex = function (count) {
        var gen = this._indexGen;
        if (!gen) return 0;
        if (typeof gen === 'function') return gen(count);
        for (var i = gen.length - 1; i >= 0; i--) {
            if (count >= gen[i]) return i;
        }
        return 0;
    };

    MarkerClustering.prototype._getIcon = function (index) {
        if (!this._icons || this._icons.length === 0) return null;
        return this._icons[Math.min(index, this._icons.length - 1)];
    };

    global.MarkerClustering = MarkerClustering;

}(window));
