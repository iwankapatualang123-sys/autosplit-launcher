/* AutoSplit: menyesuaikan tata letak template ELIO dengan ukuran area agar tidak ada ruang terbuang.
   Area tinggi: kotak sensor dipanjangkan, isinya direnggangkan dan diperbesar sebisa muat, pod HUD dan
   jam bulat diperbesar lalu ditaruh di tengah. Area lebar: kotak sensor ke tepi. BMW M3: direntangkan
   penuh dengan teks tetap proporsional. Template asli tidak diubah; semua atribut bisa dikembalikan. */
(function () {
  'use strict';
  var BW = 440, BH = 340, TEXT_MAX = 1.3, CENTER_W = 204;
  var touched = [];

  function num(v) { return parseFloat(v) || 0; }
  function tr(t) {
    var m = /translate\(\s*([-\d.]+)[ ,]+([-\d.]+)\s*\)/.exec(t || '');
    return m ? [+m[1], +m[2]] : [0, 0];
  }
  function orig(el, name) {
    var k = 'data-o-' + name;
    if (!el.hasAttribute(k)) {
      var v = el.getAttribute(name);
      el.setAttribute(k, v === null ? '\u0000' : v);
    }
    var o = el.getAttribute(k);
    return o === '\u0000' ? null : o;
  }
  function touch(el, name, value) {
    orig(el, name);
    el.setAttribute(name, value);
    touched.push([el, name]);
  }
  function resetAll() {
    touched.forEach(function (p) {
      var o = orig(p[0], p[1]);
      if (o === null) p[0].removeAttribute(p[1]); else p[0].setAttribute(p[1], o);
    });
    touched = [];
  }
  function scalePoints(points, fy) {
    return points.trim().split(/\s+/).map(function (pt) {
      var xy = pt.split(',');
      return xy[0] + ',' + (num(xy[1]) * fy).toFixed(1);
    }).join(' ');
  }
  function hasBigCircle(g) {
    var cs = g.querySelectorAll('circle');
    for (var i = 0; i < cs.length; i++) if (num(cs[i].getAttribute('r')) >= 80) return true;
    return false;
  }

  function stretchWing(g, gt, H) {
    var bg = g.querySelector('.elio-wing');
    var bb = bg.getBBox();
    var newH = H - gt[1] - 6;
    var fy = newH / bb.height;
    if (fy <= 1.01) return;
    if (bg.tagName === 'rect') touch(bg, 'height', newH.toFixed(1));
    else touch(bg, 'points', scalePoints(orig(bg, 'points'), fy));
    var innerR = bb.x + bb.width - 6;
    Array.prototype.forEach.call(g.children, function (c) {
      if (c === bg) return;
      var cb = c.getBBox();
      var ot = orig(c, 'transform');
      var ct = tr(ot);
      var x = cb.x + ct[0], y = cb.y + ct[1];
      if (y < 2 && cb.height < 3) return; // garis aksen atas tetap di atas
      var dy = (y + cb.height / 2) * (fy - 1);
      var tf = 'translate(0 ' + dy.toFixed(1) + ')';
      if ((c.tagName === 'text' || c.tagName === 'g') && cb.width > 0) {
        var t = Math.min(TEXT_MAX, fy, (innerR - x) / cb.width);
        if (t > 1.01) {
          var ax = x, ay = y + cb.height;
          tf += ' translate(' + ax + ' ' + ay + ') scale(' + t.toFixed(3) + ') translate(' + (-ax) + ' ' + (-ay) + ')';
        }
      }
      touch(c, 'transform', (tf + ' ' + (ot || '')).trim());
    });
  }

  function layoutTall(svg, kids, H) {
    var pod = null, dial = null, laser = null;
    kids.forEach(function (e) {
      var ot = orig(e, 'transform'), t = tr(ot), b = e.getBBox();
      var by = b.y + t[1];
      if (e.tagName === 'g' && e.querySelector('.elio-wing') && b.width <= 125 && b.height > 250) {
        stretchWing(e, t, H);
      } else if (e.tagName === 'g' && hasBigCircle(e)) {
        dial = e;
      } else if (e.tagName === 'line' && e.getAttribute('stroke-dasharray')) {
        laser = e;
      } else if (e.tagName === 'g' && by < 12 && b.height < 100 && b.width < 200) {
        pod = e;
      } else if (e.tagName === 'g' && e.querySelector('.elio-wing') && b.height > 250) {
        var fyB = (H - t[1] - 6) / b.height;
        touch(e, 'transform', 'translate(' + t[0] + ' ' + t[1] + ') scale(1 ' + fyB.toFixed(3) + ')');
      }
    });

    var podBottom = 0;
    if (pod) {
      var pb = pod.getBBox();
      var kp = Math.min(1.25, H / BH, CENTER_W / pb.width);
      touch(pod, 'transform', 'translate(220 6) scale(' + kp.toFixed(3) + ') translate(-220 -6) ' + (orig(pod, 'transform') || ''));
      podBottom = 6 + (pb.y + pb.height - 6) * kp;
    }
    if (dial) {
      var dr = dial.getBBox().width / 2;
      var top = podBottom + 14, avail = H - 8 - top;
      var k = Math.max(1, Math.min(CENTER_W / (2 * dr), avail / (2 * dr)));
      var cy = top + avail / 2;
      touch(dial, 'transform', 'translate(220 ' + cy.toFixed(1) + ') scale(' + k.toFixed(3) + ')');
      if (laser) {
        touch(laser, 'y1', (podBottom + 2).toFixed(1));
        touch(laser, 'y2', (cy - dr * k - 2).toFixed(1));
      }
    }
  }

  function layoutWide(kids, dx) {
    kids.forEach(function (e) {
      var ot = orig(e, 'transform'), t = tr(ot), b = e.getBBox();
      var cx = b.x + t[0] + b.width / 2;
      var shift = cx < 140 ? 0 : (cx > 300 ? dx : dx / 2);
      if (shift) touch(e, 'transform', ('translate(' + shift.toFixed(1) + ' 0) ' + (ot || '')).trim());
    });
  }

  function layoutM3(svg, w, h) {
    svg.setAttribute('preserveAspectRatio', 'none');
    var sx = w / BW, sy = h / BH, s = Math.min(sx, sy);
    Array.prototype.forEach.call(svg.querySelectorAll('text'), function (t) {
      var x = num(t.getAttribute('x')), y = num(t.getAttribute('y'));
      touch(t, 'transform', 'translate(' + x + ' ' + y + ') scale(' + (s / sx).toFixed(4) + ' ' + (s / sy).toFixed(4) +
        ') translate(' + (-x) + ' ' + (-y) + ')');
    });
  }

  function layout() {
    if (!window.ElioSpeedo) return;
    resetAll();
    var box = document.getElementById('cluster_' + window.ElioSpeedo.style);
    if (!box) return;
    var svg = box.querySelector('svg');
    if (!svg) return;
    var w = svg.clientWidth || svg.getBoundingClientRect().width;
    var h = svg.clientHeight || svg.getBoundingClientRect().height;
    if (w < 10 || h < 10) return;
    if (box.id === 'cluster_m3') { layoutM3(svg, w, h); return; }

    var kids = Array.prototype.filter.call(svg.children, function (e) { return e.tagName !== 'defs'; });
    var a = w / h;
    if (a < BW / BH - 0.01) {
      var H = BW / a;
      touch(svg, 'viewBox', '0 0 ' + BW + ' ' + H.toFixed(1));
      layoutTall(svg, kids, H);
    } else if (a > BW / BH + 0.01) {
      var W = BH * a;
      touch(svg, 'viewBox', '0 0 ' + W.toFixed(1) + ' ' + BH);
      layoutWide(kids, W - BW);
    }
  }

  var pending = false;
  function schedule() {
    if (pending) return;
    pending = true;
    requestAnimationFrame(function () { pending = false; layout(); });
  }

  if (window.ElioSpeedo) {
    var baseSetStyle = window.ElioSpeedo.setStyle;
    window.ElioSpeedo.setStyle = function (name) { baseSetStyle(name); schedule(); };
  }
  window.addEventListener('resize', schedule);
  window.AutoSplitLayout = { refresh: schedule };
  schedule();
})();
