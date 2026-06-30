// native bridge for macOS sandbox-friendly operations, called from the jvm via jni
#import <Foundation/Foundation.h>
#import <AppKit/AppKit.h>
#import <Security/Security.h>
#import <jni.h>

static NSString *kService = @"BackupX";

static NSString *jstringToNS(JNIEnv *env, jstring s) {
    if (!s) return nil;
    const char *c = (*env)->GetStringUTFChars(env, s, NULL);
    NSString *r = [NSString stringWithUTF8String:c];
    (*env)->ReleaseStringUTFChars(env, s, c);
    return r;
}

static jstring nsToJstring(JNIEnv *env, NSString *s) {
    if (!s) return NULL;
    return (*env)->NewStringUTF(env, [s UTF8String]);
}

JNIEXPORT jboolean JNICALL Java_com_backupx_app_helper_MacNativeBridge_keychainStore(JNIEnv *env, jobject thiz, jstring jaccount, jstring jsecret) {
    @autoreleasepool {
        NSString *account = jstringToNS(env, jaccount);
        NSString *secret = jstringToNS(env, jsecret);
        if (!account || !secret) return JNI_FALSE;

        NSDictionary *query = @{ (id)kSecClass: (id)kSecClassGenericPassword,
                                 (id)kSecAttrService: kService,
                                 (id)kSecAttrAccount: account };
        // replace any existing entry for this account
        SecItemDelete((__bridge CFDictionaryRef)query);

        NSMutableDictionary *add = [query mutableCopy];
        add[(id)kSecValueData] = [secret dataUsingEncoding:NSUTF8StringEncoding];
        OSStatus status = SecItemAdd((__bridge CFDictionaryRef)add, NULL);
        return status == errSecSuccess ? JNI_TRUE : JNI_FALSE;
    }
}

JNIEXPORT jstring JNICALL Java_com_backupx_app_helper_MacNativeBridge_keychainLoad(JNIEnv *env, jobject thiz, jstring jaccount) {
    @autoreleasepool {
        NSString *account = jstringToNS(env, jaccount);
        if (!account) return NULL;

        NSDictionary *query = @{ (id)kSecClass: (id)kSecClassGenericPassword,
                                 (id)kSecAttrService: kService,
                                 (id)kSecAttrAccount: account,
                                 (id)kSecReturnData: @YES,
                                 (id)kSecMatchLimit: (id)kSecMatchLimitOne };
        CFTypeRef result = NULL;
        OSStatus status = SecItemCopyMatching((__bridge CFDictionaryRef)query, &result);
        if (status != errSecSuccess || !result) return NULL;

        NSData *data = (__bridge_transfer NSData *)result;
        NSString *secret = [[NSString alloc] initWithData:data encoding:NSUTF8StringEncoding];
        return nsToJstring(env, secret);
    }
}

JNIEXPORT void JNICALL Java_com_backupx_app_helper_MacNativeBridge_keychainDelete(JNIEnv *env, jobject thiz, jstring jaccount) {
    @autoreleasepool {
        NSString *account = jstringToNS(env, jaccount);
        if (!account) return;
        NSDictionary *query = @{ (id)kSecClass: (id)kSecClassGenericPassword,
                                 (id)kSecAttrService: kService,
                                 (id)kSecAttrAccount: account };
        SecItemDelete((__bridge CFDictionaryRef)query);
    }
}

JNIEXPORT void JNICALL Java_com_backupx_app_helper_MacNativeBridge_openInFinder(JNIEnv *env, jobject thiz, jstring jpath) {
    @autoreleasepool {
        NSString *path = jstringToNS(env, jpath);
        if (!path) return;
        NSURL *url = [NSURL fileURLWithPath:path];
        // ui call, hop to the main thread without blocking
        dispatch_async(dispatch_get_main_queue(), ^{
            [[NSWorkspace sharedWorkspace] openURL:url];
        });
    }
}

JNIEXPORT jstring JNICALL Java_com_backupx_app_helper_MacNativeBridge_bookmarkCreate(JNIEnv *env, jobject thiz, jstring jpath) {
    @autoreleasepool {
        NSString *path = jstringToNS(env, jpath);
        if (!path) return NULL;
        NSURL *url = [NSURL fileURLWithPath:path];

        NSError *error = nil;
        NSData *bookmark = [url bookmarkDataWithOptions:NSURLBookmarkCreationWithSecurityScope
                         includingResourceValuesForKeys:nil
                                          relativeToURL:nil
                                                  error:&error];
        if (!bookmark) {
            // outside the sandbox the security scope may be unavailable, fall back to a plain bookmark
            error = nil;
            bookmark = [url bookmarkDataWithOptions:0
                     includingResourceValuesForKeys:nil
                                      relativeToURL:nil
                                              error:&error];
        }
        if (!bookmark) return NULL;
        return nsToJstring(env, [bookmark base64EncodedStringWithOptions:0]);
    }
}

JNIEXPORT jstring JNICALL Java_com_backupx_app_helper_MacNativeBridge_bookmarkResolve(JNIEnv *env, jobject thiz, jstring jbookmark) {
    @autoreleasepool {
        NSString *base64 = jstringToNS(env, jbookmark);
        if (!base64) return NULL;
        NSData *bookmark = [[NSData alloc] initWithBase64EncodedString:base64 options:0];
        if (!bookmark) return NULL;

        BOOL stale = NO;
        NSError *error = nil;
        NSURL *url = [NSURL URLByResolvingBookmarkData:bookmark
                                               options:NSURLBookmarkResolutionWithSecurityScope
                                         relativeToURL:nil
                                   bookmarkDataIsStale:&stale
                                                 error:&error];
        if (!url) {
            error = nil;
            url = [NSURL URLByResolvingBookmarkData:bookmark
                                            options:0
                                      relativeToURL:nil
                                bookmarkDataIsStale:&stale
                                              error:&error];
        }
        if (!url) return NULL;
        // start access and keep it for the run, no-op when the bookmark is not security scoped
        [url startAccessingSecurityScopedResource];
        return nsToJstring(env, [url path]);
    }
}
