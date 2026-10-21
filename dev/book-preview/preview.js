(function () {
  var book = window.EXPLORATION_BOOK;
  var status = document.getElementById("status");
  var warning = document.getElementById("warning");
  var pageLabel = document.getElementById("page-label");
  var noticeList = document.getElementById("notice-list");
  var prev = document.getElementById("prev");
  var next = document.getElementById("next");
  var canvas = document.getElementById("book");
  var ctx = canvas.getContext("2d");
  var scale = 4;
  var pageIndex = 0;
  var ascii = null;
  var fonts = {};
  var cover = null;
  var arrows = {};
  var hover = null;

  if (!book) {
    status.textContent = "book.js is missing. Run gradlew bookPreview, then refresh.";
    prev.disabled = true;
    next.disabled = true;
    return;
  }

  status.textContent = book.title + " — " + book.author;
  if (book.notices.length === 0) {
    var quiet = document.createElement("li");
    quiet.textContent = "No notices. The server log would stay quiet.";
    noticeList.appendChild(quiet);
  } else {
    book.notices.forEach(function (notice) {
      var item = document.createElement("li");
      item.textContent = notice;
      noticeList.appendChild(item);
    });
  }

  canvas.width = book.imageWidth * scale;
  canvas.height = book.imageHeight * scale;
  ctx.imageSmoothingEnabled = false;

  prev.addEventListener("click", function () { turn(-1); });
  next.addEventListener("click", function () { turn(1); });
  document.addEventListener("keydown", function (event) {
    if (event.key === "ArrowLeft" || event.key === "PageUp") {
      turn(-1);
    } else if (event.key === "ArrowRight" || event.key === "PageDown") {
      turn(1);
    }
  });
  canvas.addEventListener("mousemove", function (event) {
    var point = bookPoint(event);
    var hit = buttonAt(point.x, point.y);
    if (hit !== hover) {
      hover = hit;
      paint();
    }
  });
  canvas.addEventListener("mouseleave", function () {
    if (hover !== null) {
      hover = null;
      paint();
    }
  });
  canvas.addEventListener("click", function (event) {
    var point = bookPoint(event);
    if (buttonAt(point.x, point.y) === "previous") {
      turn(-1);
      return;
    }
    if (buttonAt(point.x, point.y) === "next") {
      turn(1);
      return;
    }
    turn(point.x >= book.imageWidth / 2 ? 1 : -1);
  });

  Promise.all([
    loadImage("assets/ascii.png"),
    loadImage("assets/book.png"),
    loadImage("assets/page_forward.png"),
    loadImage("assets/page_forward_highlighted.png"),
    loadImage("assets/page_backward.png"),
    loadImage("assets/page_backward_highlighted.png")
  ]).then(function (images) {
    ascii = images[0];
    cover = images[1];
    arrows.forward = images[2];
    arrows.forwardHot = images[3];
    arrows.back = images[4];
    arrows.backHot = images[5];
    if (!ascii || !cover) {
      warning.textContent = "ascii.png or book.png is missing, so this is a stand-in page. Run gradlew bookPreview -PminecraftJar=path\\to\\client.jar.";
    }
    paint();
  });

  function turn(delta) {
    var nextIndex = pageIndex + delta;
    if (nextIndex < 0 || nextIndex >= book.pages.length) {
      return;
    }
    pageIndex = nextIndex;
    paint();
  }

  function paint() {
    var lines = book.pages[pageIndex];
    ctx.clearRect(0, 0, canvas.width, canvas.height);
    if (cover) {
      ctx.drawImage(cover, 0, 0, book.imageWidth, book.imageHeight, 0, 0, canvas.width, canvas.height);
    } else {
      ctx.fillStyle = "#c6b48a";
      ctx.fillRect(0, 0, canvas.width, canvas.height);
      ctx.fillStyle = "#6b4428";
      ctx.fillRect(0, 0, 18 * scale, canvas.height);
    }

    var indicator = book.pageIndicator.replace("%s", String(pageIndex + 1)).replace("%s", String(book.pages.length));
    var indicatorWidth = measure(indicator);
    drawLine(indicator, book.imageWidth - book.pageIndicatorRight - indicatorWidth, book.pageIndicatorY);

    var visible = Math.min(lines.length, book.linesPerPage);
    for (var index = 0; index < visible; index++) {
      drawLine(lines[index], book.textX, book.textY + index * book.lineHeight);
    }

    drawButton(book.previousButton, "previous", pageIndex > 0);
    drawButton(book.nextButton, "next", pageIndex < book.pages.length - 1);

    pageLabel.textContent = indicator;
    prev.disabled = pageIndex === 0;
    next.disabled = pageIndex === book.pages.length - 1;
    if (lines.length > book.linesPerPage) {
      warning.textContent = "Page " + (pageIndex + 1) + " has " + lines.length + " lines. The book shows " + book.linesPerPage + ".";
    } else if (ascii && cover) {
      warning.textContent = "";
    }
  }

  function drawButton(button, name, enabled) {
    if (!enabled) {
      return;
    }
    var sprite = arrowSprite(name, hover === name);
    if (sprite) {
      ctx.drawImage(sprite, button.x * scale, button.y * scale, button.w * scale, button.h * scale);
      return;
    }
    if (!cover) {
      return;
    }
    var u = button.u + (hover === name ? book.buttonHoverU : 0);
    ctx.drawImage(
      cover,
      u, button.v, button.w, button.h,
      button.x * scale, button.y * scale, button.w * scale, button.h * scale
    );
  }

  function arrowSprite(name, hot) {
    if (name === "next") {
      return hot ? arrows.forwardHot : arrows.forward;
    }
    return hot ? arrows.backHot : arrows.back;
  }

  function drawLine(line, x, y) {
    var style = lineStyle(line);
    var text = style.text;
    var glyphs = fontFor(style.color);
    var pen = x;
    for (var index = 0; index < text.length; ) {
      var codePoint = text.codePointAt(index);
      var advance = glyphAdvance(codePoint, style.bold);
      if (glyphs && codePoint <= 255) {
        blitGlyph(glyphs, codePoint, pen, y);
        if (style.bold) {
          blitGlyph(glyphs, codePoint, pen + 1, y);
        }
      } else if (codePoint !== 32) {
        var width = Math.max(advanceOf(codePoint) - 1, 1);
        ctx.fillStyle = style.color;
        ctx.fillRect(pen * scale, (y + 1) * scale, width * scale, 7 * scale);
        if (style.bold) {
          ctx.fillRect((pen + 1) * scale, (y + 1) * scale, width * scale, 7 * scale);
        }
      }
      pen += advance;
      index += codePoint > 0xffff ? 2 : 1;
    }
  }

  function lineStyle(line) {
    if (typeof line === "string") {
      return { text: line, bold: false, color: "#000000" };
    }
    return {
      text: line.text,
      bold: !!line.bold,
      color: line.color ? "#" + line.color : "#000000"
    };
  }

  function blitGlyph(glyphs, codePoint, pen, y) {
    var column = codePoint % 16;
    var row = Math.floor(codePoint / 16);
    ctx.drawImage(
      glyphs,
      column * book.glyphSize,
      row * book.glyphSize,
      book.glyphSize,
      book.glyphSize,
      pen * scale,
      y * scale,
      book.glyphSize * scale,
      book.glyphSize * scale
    );
  }

  function glyphAdvance(codePoint, bold) {
    return advanceOf(codePoint) + (bold ? 1 : 0);
  }

  function fontFor(color) {
    if (!ascii) {
      return null;
    }
    if (!fonts[color]) {
      fonts[color] = tint(ascii, color);
    }
    return fonts[color];
  }

  function measure(text) {
    var width = 0;
    for (var index = 0; index < text.length; ) {
      var codePoint = text.codePointAt(index);
      width += advanceOf(codePoint);
      index += codePoint > 0xffff ? 2 : 1;
    }
    return width;
  }

  function advanceOf(codePoint) {
    if (codePoint >= 32 && codePoint <= 126) {
      return book.advances[codePoint - 32];
    }
    return book.unknownAdvance;
  }

  function buttonAt(x, y) {
    if (!arrows.forward && !cover) {
      return null;
    }
    if (pageIndex > 0 && inside(book.previousButton, x, y)) {
      return "previous";
    }
    if (pageIndex < book.pages.length - 1 && inside(book.nextButton, x, y)) {
      return "next";
    }
    return null;
  }

  function inside(button, x, y) {
    return x >= button.x && x < button.x + button.w && y >= button.y && y < button.y + button.h;
  }

  function bookPoint(event) {
    var rect = canvas.getBoundingClientRect();
    return {
      x: (event.clientX - rect.left) * (canvas.width / rect.width) / scale,
      y: (event.clientY - rect.top) * (canvas.height / rect.height) / scale
    };
  }

  function loadImage(src) {
    return new Promise(function (resolve) {
      var image = new Image();
      image.onload = function () { resolve(image); };
      image.onerror = function () { resolve(null); };
      image.src = src;
    });
  }

  function tint(image, color) {
    var tinted = document.createElement("canvas");
    tinted.width = image.width;
    tinted.height = image.height;
    var tintContext = tinted.getContext("2d");
    tintContext.drawImage(image, 0, 0);
    tintContext.globalCompositeOperation = "source-in";
    tintContext.fillStyle = color;
    tintContext.fillRect(0, 0, tinted.width, tinted.height);
    return tinted;
  }
})();
