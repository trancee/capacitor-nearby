export interface NearbyPlugin {
  echo(options: { value: string }): Promise<{ value: string }>;
}
