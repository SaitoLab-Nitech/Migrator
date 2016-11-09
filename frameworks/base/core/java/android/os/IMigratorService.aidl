//frameworks/base/core/java/android/os/IMigratorService.aidl
/*
 * MIT License
 * 
 * Copyright (c) 2016 SaitoLab-Nitech
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package android.os;

import android.content.Context;
import android.os.Bundle;
import java.util.List;

interface IMigratorService {
	boolean migrate(in Bundle restoredState, in String packageName, in String className, in boolean flag);
	void execMigrate(in int which);
	boolean isAvailable();
	void wipeList();
	void setStatus(in String appName, in boolean status);
	boolean checkMigratableApp();
	List<String> getDeviceList();
	void setFileNames(in String[] names);
	//void setFileBody(in List bodies);
}
