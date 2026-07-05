#import <Foundation/Foundation.h>

#if __has_include(<UIKit/UIKit.h>)
#import <UIKit/UIKit.h>

// Registers a launch observer as early as the binary is loaded so the relay can
// resume automatically when iOS relaunches the app in the background (for
// example, after a significant-location-change event) without any JavaScript
// having run yet.
@interface RelayLaunchBootstrap : NSObject
@end

@implementation RelayLaunchBootstrap

+ (void)load {
  [[NSNotificationCenter defaultCenter]
      addObserverForName:UIApplicationDidFinishLaunchingNotification
                  object:nil
                   queue:[NSOperationQueue mainQueue]
              usingBlock:^(NSNotification *_Nonnull note) {
                Class resumer = NSClassFromString(@"RelayLaunchResumer");
                if (resumer != nil &&
                    [resumer respondsToSelector:@selector(resume)]) {
#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Warc-performSelector-leaks"
                  [resumer performSelector:@selector(resume)];
#pragma clang diagnostic pop
                }
              }];
}

@end

#endif
