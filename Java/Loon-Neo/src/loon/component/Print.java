package loon.component;

import loon.LRelease;
import loon.LSystem;
import loon.LTexture;
import loon.canvas.LColor;
import loon.event.Updateable;
import loon.font.FontSet;
import loon.font.IFont;
import loon.font.LFont;
import loon.geom.Vector2f;
import loon.opengl.GLEx;
import loon.opengl.LSTRDictionary;
import loon.opengl.LSTRFont;
import loon.utils.StringUtils;
import loon.utils.TArray;

public class Print implements FontSet<Print>, LRelease {

	public enum Mode {
		NONE, LEFT, RIGHT, CENTER
	}

	// they is other char flags
	private final static char[] _wrapchars = { '\u3002', '\u3001', '\uff0c',
			'\uff0e', '\u300d', '\uff3d', '\u3011', '\u300f', '\u30fc',
			'\uff5e', '\uff09', '\u3041', '\u3043', '\u3045', '\u3047',
			'\u3049', '\u30a1', '\u30a3', '\u30a5', '\u30a7', '\u30a9',
			'\u30c3', '\u30e3', '\u30e5', '\u30e7', '\u30ee', '\u308e',
			'\u3083', '\u3085', '\u3087', '\u3063', '\u2026', '\uff0d',
			'\uff01', '\uff1f' };

	private final static int _otherFlagsSize = _wrapchars.length;

	/**
	 * 返回指定字符串，匹配指定字体后，在指定宽度内的每行应显示字符串.
	 * 
	 * PS:此项不处理'\n'外的特殊操作符
	 * 
	 * @param text
	 * @param font
	 * @param width
	 * @return
	 */
	public static TArray<String> formatMessage(String text, IFont font,
			int width) {
		TArray<String> list = new TArray<String>();

		if (text == null) {
			return list;
		}

		char c1 = '〜';
		char c2 = 65374;
		String str = text.replace(c1, c2);
		String line = "";

		int i = 0;

		while (i <= str.length()) {
			if (i == str.length()) {
				list.add(line);
				break;
			}

			char c = str.charAt(i);

			if ((c == '\n') || (font.stringWidth(line + c) > width)) {
				line = str.substring(0, i);

				for (int j = 0; j < _otherFlagsSize; j++) {
					if (c == _wrapchars[j]) {
						int delta = font.stringWidth(line + c) - width;
						if (delta < 15) {
							line = str.substring(0, ++i);
							break;
						}
					}
				}
				i += (c == '\n' ? 1 : 0);
				list.add(line);
				line = "";
				str = str.substring(i);
				i = 0;
			} else {
				line = line + c;
				i++;
			}
		}

		return list;
	}

	private int index, offset, font, tmp_font;

	private char text;

	private char[] showMessages;

	private LColor fontColor = new LColor(LColor.white);

	private int interceptMaxString;

	private int interceptCount;

	private int messageLength = 10;

	private String messages;

	private boolean onComplete, newLine, visible;

	private StringBuffer messageBuffer = new StringBuffer(messageLength);

	private int width, height, leftOffset, topOffset, next, messageCount;

	private float alpha;

	private int size, wait, tmp_dir, left, fontSize, fontHeight;

	private Vector2f vector;

	private LTexture creeseIcon;

	private LSTRFont strings;

	private IFont ifont;

	private boolean isEnglish, isWait;

	private float iconX, iconY;

	private int lazyHashCade = 1;

	// 默认0，左1,右2
	private Mode dirmode = Mode.NONE;

	public Print(Vector2f vector, IFont font, int width, int height) {
		this("", font, vector, width, height);
	}

	public Print(String context, IFont font, Vector2f vector, int width,
			int height) {
		this.setMessage(context, font);
		this.vector = vector;
		this.width = width;
		this.height = height;
		this.wait = 0;
		this.isWait = false;
	}

	private boolean nativeFont = false;

	public void setMessage(String context, IFont font) {
		setMessage(context, font, false);
	}

	private class PrintUpdate implements Updateable {

		Print _print;

		boolean _isComplete = false, _drawDrawingFont = false;

		private IFont _font = null;

		private String _context = null;

		private PrintUpdate(Print print, String context, IFont font,
				boolean isComplete, boolean drawFont) {
			_print = print;
			_context = context;
			_font = font;
			_isComplete = isComplete;
			_drawDrawingFont = drawFont;
		}

		@Override
		public void action(Object a) {
			if (_context == null) {
				return;
			}
			if (_print.strings != null && !_print.strings.isClose()
					&& !_drawDrawingFont) {
				_print.strings.close();
			}
			// 如果是默认的loon系统字体
			if (_font instanceof LFont) {
				if (_drawDrawingFont) {
					LSTRDictionary.Dict dict = LSTRDictionary.get().bind(
							(LFont) _font, _context);
					_print.strings = dict.getSTR();
					_print.ifont = _font;
				} else {
					_print.strings = new LSTRFont((LFont) _font, _context,
							LSystem.isHTML5());
				}
				// 其他字体(一般是Bitmap Font)
			} else {
				_print.ifont = _font;
			}
			_print.lazyHashCade = 1;
			_print.wait = 0;
			_print.visible = false;
			_print.showMessages = new char[] { '\0' };
			_print.interceptMaxString = 0;
			_print.next = 0;
			_print.messageCount = 0;
			_print.interceptCount = 0;
			_print.size = 0;
			_print.tmp_dir = 0;
			_print.left = 0;
			_print.fontSize = 0;
			_print.fontHeight = 0;
			_print.messages = _context;
			_print.next = _context.length();
			_print.onComplete = false;
			_print.newLine = false;
			_print.messageCount = 0;
			_print.messageBuffer.delete(0, messageBuffer.length());
			if (_isComplete) {
				_print.complete();
			}
			_print.visible = true;
		}
	}

	public void setMessage(String context, IFont font, boolean isComplete) {
		setMessage(context, font, isComplete, false);
	}

	public void setMessage(String context, IFont font, boolean isComplete,
			boolean drawFont) {
		LSystem.load(new PrintUpdate(this, context, font, isComplete,
				this.nativeFont = drawFont));
	}

	public String getMessage() {
		return messages;
	}

	private LColor getColor(char flagName) {
		if ('r' == flagName || 'R' == flagName) {
			return LColor.red;
		} else if ('b' == flagName || 'B' == flagName) {
			return LColor.black;
		} else if ('l' == flagName || 'L' == flagName) {
			return LColor.blue;
		} else if ('g' == flagName || 'G' == flagName) {
			return LColor.green;
		} else if ('o' == flagName || 'O' == flagName) {
			return LColor.orange;
		} else if ('y' == flagName || 'Y' == flagName) {
			return LColor.yellow;
		} else if ('m' == flagName || 'M' == flagName) {
			return LColor.magenta;
		} else if ('d' == flagName || 'D' == flagName) {
			return LColor.darkGray;
		} else if ('e' == flagName || 'E' == flagName) {
			return LColor.green;
		} else if ('p' == flagName || 'P' == flagName) {
			return LColor.pink;
		}
		return null;
	}

	public void draw(GLEx g) {
		draw(g, LColor.white);
	}

	private void drawMessage(GLEx gl, LColor old) {
		if (!visible) {
			return;
		}
		if ((strings == null && ifont != null) || nativeFont) {
			drawBMFont(gl, old);
		} else if (strings != null) {
			drawDefFont(gl, old);
		}
	}

	public void drawDefFont(GLEx g, LColor old) {
		synchronized (showMessages) {
			this.size = showMessages.length;
			this.fontSize = (int) (isEnglish ? strings.getSize() / 2 : strings
					.getSize());
			this.fontHeight = strings.getHeight();
			switch (dirmode) {
			default:
			case NONE:
				this.tmp_dir = 2;
				break;
			case LEFT:
				this.tmp_dir = (width - (fontSize * messageLength)) / 2
						- (int) (fontSize * 1.5);
				break;
			case RIGHT:
				this.tmp_dir = (fontSize * messageLength) / 2;
				break;
			case CENTER:
				this.tmp_dir = width / 2 - (fontSize * messageLength) / 2
						+ (int) (fontSize * 4);
				break;
			}
			this.left = tmp_dir;
			this.index = offset = font = tmp_font = 0;

			int hashCode = 1;
			hashCode = LSystem.unite(hashCode, size);
			hashCode = LSystem.unite(hashCode, left);
			hashCode = LSystem.unite(hashCode, fontSize);
			hashCode = LSystem.unite(hashCode, fontHeight);

			if (strings == null) {
				return;
			}

			if (hashCode == lazyHashCade) {
				strings.postCharCache();
				if (iconX != 0 && iconY != 0) {
					g.draw(creeseIcon, iconX, iconY);
				}
				return;
			}

			strings.startChar();
			fontColor = old;

			for (int i = 0; i < size; i++) {
				text = showMessages[i];
				if (text == '\0') {
					continue;
				}
				if (interceptCount < interceptMaxString) {
					interceptCount++;
					continue;
				} else {
					interceptMaxString = 0;
					interceptCount = 0;
				}
				if (showMessages[i] == 'n'
						&& showMessages[i > 0 ? i - 1 : 0] == '\\') {
					index = 0;
					left = tmp_dir;
					offset++;
					continue;
				} else if (text == '\n') {
					index = 0;
					left = tmp_dir;
					offset++;
					continue;
				} else if (text == '<') {
					LColor color = getColor(showMessages[i < size - 1 ? i + 1
							: i]);
					if (color != null) {
						interceptMaxString = 1;
						fontColor = color;
					}
					continue;
				} else if (showMessages[i > 0 ? i - 1 : i] == '<'
						&& getColor(text) != null) {
					continue;
				} else if (text == '/') {
					if (showMessages[i < size - 1 ? i + 1 : i] == '>') {
						interceptMaxString = 1;
						fontColor = old;
					}
					continue;
				} else if (index > messageLength) {
					index = 0;
					left = tmp_dir;
					offset++;
					newLine = false;
				} else if (text == '\\') {
					continue;
				}
				tmp_font = strings.charWidth(text);
				if (Character.isLetter(text)) {
					if (tmp_font < fontSize) {
						font = fontSize;
					} else {
						font = tmp_font;
					}
				} else {
					font = fontSize;
				}
				left += font;
				if (font <= 10 && StringUtils.isSingle(text)) {
					left += 12;
				}
				if (i != size - 1) {
					strings.addChar(text, vector.x + left + leftOffset,
							(offset * fontHeight) + vector.y + fontSize
									+ topOffset, fontColor);
				} else if (!newLine && !onComplete) {
					iconX = vector.x + left + leftOffset;
					iconY = (offset * fontHeight) + vector.y + fontSize
							+ topOffset + strings.getAscent();
					if (iconX != 0 && iconY != 0) {
						g.draw(creeseIcon, iconX, iconY);
					}
				}
				index++;
			}

			strings.stopChar();
			strings.saveCharCache();

			lazyHashCade = hashCode;

			if (messageCount == next) {
				onComplete = true;
			}
		}
	}

	public void drawBMFont(GLEx g, LColor old) {
		synchronized (showMessages) {
			this.size = showMessages.length;
			if (nativeFont) {
				this.fontSize = (int) (isEnglish ? strings.getSize() / 2
						: ifont.getSize());
				this.fontHeight = strings.getHeight();
			} else {
				this.fontSize = (int) (isEnglish ? ifont.getSize() / 2 : ifont
						.getSize());
				this.fontHeight = ifont.getHeight();
			}
			switch (dirmode) {
			default:
			case NONE:
				this.tmp_dir = 0;
				break;
			case LEFT:
				this.tmp_dir = (width - (fontSize * messageLength)) / 2
						- (int) (fontSize * 1.5);
				break;
			case RIGHT:
				this.tmp_dir = (fontSize * messageLength) / 2;
				break;
			case CENTER:
				this.tmp_dir = width / 2 - (fontSize * messageLength) / 2
						+ (int) (fontSize * 4);
				break;
			}
			this.left = tmp_dir;
			this.index = offset = font = tmp_font = 0;
			fontColor = old;
			for (int i = 0; i < size; i++) {
				text = showMessages[i];
				if (text == '\0') {
					continue;
				}
				if (interceptCount < interceptMaxString) {
					interceptCount++;
					continue;
				} else {
					interceptMaxString = 0;
					interceptCount = 0;
				}
				if (showMessages[i] == 'n'
						&& showMessages[i > 0 ? i - 1 : 0] == '\\') {
					index = 0;
					left = tmp_dir;
					offset++;
					continue;
				} else if (text == '\n') {
					index = 0;
					left = tmp_dir;
					offset++;
					continue;
				} else if (text == '<') {
					LColor color = getColor(showMessages[i < size - 1 ? i + 1
							: i]);
					if (color != null) {
						interceptMaxString = 1;
						fontColor = color;
					}
					continue;
				} else if (showMessages[i > 0 ? i - 1 : i] == '<'
						&& getColor(text) != null) {
					continue;
				} else if (text == '/') {
					if (showMessages[i < size - 1 ? i + 1 : i] == '>') {
						interceptMaxString = 1;
						fontColor = old;
					}
					continue;
				} else if (index > messageLength) {
					index = 0;
					left = tmp_dir;
					offset++;
					newLine = false;
				} else if (text == '\\') {
					continue;
				}
				String tmpText = String.valueOf(text);
				tmp_font = ifont.charWidth(text);
				if (Character.isLetter(text)) {
					if (tmp_font < fontSize) {
						font = fontSize;
					} else {
						font = tmp_font;
					}
				} else {
					font = fontSize;
				}
				left += font;
				if (font <= 10 && StringUtils.isSingle(text)) {
					left += 12;
				}
				if (i != size - 1) {
					ifont.drawString(g, tmpText, vector.x + left + leftOffset,
							(offset * fontHeight) + vector.y + fontSize
									+ topOffset, fontColor);
				} else if (!newLine && !onComplete) {
					iconX = vector.x + left + leftOffset;
					iconY = (offset * fontHeight) + vector.y + fontSize
							+ topOffset + ifont.getAscent();
					if (iconX != 0 && iconY != 0) {
						g.draw(creeseIcon, iconX, iconY);
					}
				}
				index++;
			}
			if (onComplete) {
				if (iconX != 0 && iconY != 0) {
					g.draw(creeseIcon, iconX, iconY);
				}
			}
			if (messageCount == next) {
				onComplete = true;
			}

		}

	}

	public synchronized void draw(GLEx g, LColor old) {
		if (!visible) {
			return;
		}
		alpha = g.alpha();
		if (alpha != 1f) {
			g.setAlpha(1f);
		}
		drawMessage(g, old);
		if (alpha != 1f) {
			g.setAlpha(alpha);
		}
	}

	public void setX(int x) {
		vector.setX(x);
	}

	public void setY(int y) {
		vector.setY(y);
	}

	public int getX() {
		return vector.x();
	}

	public int getY() {
		return vector.y();
	}

	public void complete() {
		synchronized (showMessages) {
			this.onComplete = true;
			this.messageCount = messages.length();
			this.next = messageCount;
			this.showMessages = (messages + "_").toCharArray();
			this.size = showMessages.length;
		}
	}

	public boolean isComplete() {
		if (isWait) {
			if (onComplete) {
				wait++;
			}
			return onComplete && wait > 100;
		}
		return onComplete;
	}

	public boolean next() {
		synchronized (messageBuffer) {
			if (!onComplete) {
				if (messageCount == next) {
					onComplete = true;
					return false;
				}
				if (messageBuffer.length() > 0) {
					messageBuffer.delete(messageBuffer.length() - 1,
							messageBuffer.length());
				}
				this.messageBuffer.append(messages.charAt(messageCount));
				this.messageBuffer.append('_');
				this.showMessages = messageBuffer.toString().toCharArray();
				this.size = showMessages.length;
				this.messageCount++;
			} else {
				return false;
			}
			return true;
		}
	}

	public LTexture getCreeseIcon() {
		return creeseIcon;
	}

	public void setCreeseIcon(LTexture icon) {
		if (this.creeseIcon != null) {
			creeseIcon.close();
			creeseIcon = null;
		}
		this.creeseIcon = icon;
		if (icon == null) {
			return;
		}
	}

	public int getMessageLength() {
		return messageLength;
	}

	public void setMessageLength(int messageLength) {
		this.messageLength = messageLength;
	}

	public int getHeight() {
		return height;
	}

	public void setHeight(int height) {
		this.height = height;
	}

	public int getWidth() {
		return width;
	}

	public void setWidth(int width) {
		this.width = width;
	}

	public int getLeftOffset() {
		return leftOffset;
	}

	public void setLeftOffset(int leftOffset) {
		this.leftOffset = leftOffset;
	}

	public int getTopOffset() {
		return topOffset;
	}

	public void setTopOffset(int topOffset) {
		this.topOffset = topOffset;
	}

	public boolean isEnglish() {
		return isEnglish;
	}

	public void setEnglish(boolean isEnglish) {
		this.isEnglish = isEnglish;
	}

	public boolean isVisible() {
		return visible;
	}

	public void setVisible(boolean visible) {
		this.visible = visible;
	}

	public Mode getTextMode() {
		return dirmode;
	}

	public void setTextMode(Mode mode) {
		this.dirmode = mode;
	}

	public void left() {
		setTextMode(Mode.LEFT);
	}

	public void right() {
		setTextMode(Mode.RIGHT);
	}

	public void center() {
		setTextMode(Mode.CENTER);
	}

	@Override
	public Print setFont(IFont font) {
		this.ifont = font;
		return this;
	}

	@Override
	public IFont getFont() {
		return ifont;
	}

	public boolean isWait() {
		return isWait;
	}

	public void setWait(boolean isWait) {
		this.isWait = isWait;
	}

	@Override
	public void close() {
		if (!nativeFont) {
			if (strings != null) {
				strings.close();
				strings = null;
			}
		}
	}

}
