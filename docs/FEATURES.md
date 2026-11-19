# FrameTube viewing controls

## SponsorBlock

SponsorBlock is optional and disabled by default. When enabled, FrameTube requests community-submitted segment timings from the public SponsorBlock API using the current video ID and the categories selected in Settings. It does not send a FrameTube account, library, subscriptions or Takeout data. The API provider receives the video ID and normal network metadata such as the device IP address.

Supported categories are sponsor, self-promotion, interaction reminders, intros, outros, previews, off-topic music and filler. Only segments marked with the API's `skip` action are used. A missing response, malformed entry or network failure does not stop playback. Community submissions can be inaccurate; users can disable any category or turn the feature off.

SponsorBlock is a third-party community service. FrameTube is not affiliated with its maintainers.

## Feed quality and recommendations

Home displays recent public uploads from channels the user has subscribed to locally. It does not claim to reproduce YouTube's personalized recommendation algorithm. The following controls are applied locally:

- Hide uploads of 60 seconds or less when duration metadata is available.
- Hide titles containing any comma-separated muted term.
- Limit how many uploads from one channel appear in the current feed list.

Unknown durations are kept rather than hidden. The settings do not modify subscriptions or remote content.

## Interface reference

FrameTube's mobile navigation uses a compact icon bar; larger windows use a navigation rail with labels. The layout and customization approach is informed by the public Metrolist project, but FrameTube keeps its own app name, red-accent palette, copy, architecture and assets. No Metrolist code or artwork is included.
