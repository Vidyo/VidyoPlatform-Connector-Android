/**
{file:
	{name: Logger.java}
	{description: .}
	{copyright:
		(c) 2016-2018 Vidyo, Inc.,
		433 Hackensack Avenue, 7th Floor,
		Hackensack, NJ  07601.

		All rights reserved.

		The information contained herein is proprietary to Vidyo, Inc.
		and shall not be reproduced, copied (in whole or in part), adapted,
		modified, disseminated, transmitted, transcribed, stored in a retrieval
		system, or translated into any language in any form by any means
		without the express written consent of Vidyo, Inc.
		                  ***** CONFIDENTIAL *****
	}
}
*/
package com.vidyo.vidyoconnector;

public class Logger {

    private static Logger mInstance = new Logger();

    public static Logger getInstance() {
        return mInstance;
    }

    private Logger() {
    }

    public void Log(String msg) {
        System.out.println("VidyoConnector App: " + msg);
    }
}