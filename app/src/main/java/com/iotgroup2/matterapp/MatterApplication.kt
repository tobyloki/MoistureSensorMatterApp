/*
 * Copyright 2022 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.iotgroup2.matterapp

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

/** Google Home Sample Application for Matter (GHSAFM) */
@HiltAndroidApp
class MatterApplication : Application() {
  override fun onCreate() {
    super.onCreate()
    // Setup Timber for logging.
    Timber.plant(Timber.DebugTree())
  }
}
