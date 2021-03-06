package com.jtransc.time;

import com.jtransc.JTranscSystem;
import com.jtransc.annotation.JTranscMethodBody;
import com.jtransc.annotation.JTranscMethodBodyList;

import com.jtransc.io.JTranscConsole;

public class JTranscClock {
	static public Impl impl = new Impl(null) {
	};

	static public class Impl {
		public Impl parent;

		public Impl(Impl parent) {
			this.parent = parent;
		}


		@JTranscMethodBody(target = "js", value = "return N.getTime();")
		@JTranscMethodBody(target = "cpp", value = "return N::getTime();")
		@JTranscMethodBody(target = "cs", value = "return N.getTime();")
		@JTranscMethodBody(target = "dart", value = "return new DateTime.now().millisecondsSinceEpoch.toDouble();")
		public double fastTime() {
			if (parent != null) {
				return parent.fastTime();
			}

			if (JTranscSystem.isJTransc()) {
				throw new RuntimeException("Not implemented JTranscSystem.fastTime()");
			} else {
				return ((double)System.nanoTime() / 1000000.0);
			}
		}

		//performance.now()
		//process.hrtime()[1] / 1000000000.0
		@JTranscMethodBody(target = "js", value = "return N.hrtime();")
		@JTranscMethodBody(target = "cpp", value = "return N::nanoTime();")
		public long nanoTime() {
			if (JTranscSystem.isJTransc()) {
				//return (long) hrtime();
				return System.currentTimeMillis() * 1000000L;
			} else {
				return System.nanoTime();
			}
		}

		@JTranscMethodBody(target = "cs", value = "System.Threading.Thread.Sleep((int)p0);")
		@JTranscMethodBody(target = "dart", value = "sleep(new Duration(milliseconds: p0.toInt()));")
		public void sleep(double ms) {
			if (parent != null) {
				parent.sleep(ms);
				return;
			}
			if (JTranscSystem.isJTransc()) {
				double start = JTranscSystem.fastTime();
				// Spinlock/Busywait!
				//JTranscConsole.log(start);
				while (true) {
					double current = JTranscSystem.fastTime();
					//JTranscConsole.log(current);
					if ((current - start) >= ms) break;
				}
			} else {
				_sleep(ms);
			}
		}

		static private void _sleep(double ms) {
			try {
				Thread.sleep((long) ms);
			} catch (Throwable t) {
			}
		}
	}
}
